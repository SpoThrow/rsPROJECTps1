package server.game.bots.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import server.game.bots.BotContext;
import server.game.bots.BotState;
import server.game.bots.BotStatus;
import server.game.bots.composite.Repeat;
import server.game.bots.composite.Sequence;
import server.game.bots.decorator.Retry;
import server.game.bots.states.BankLogs;
import server.game.bots.states.ChopTree;
import server.game.bots.states.Gather;
import server.game.bots.states.WalkTo;
import server.game.bots.world.Location;
import server.game.bots.world.Tile;

/**
 * Stage T4: the schema the editor sees is reflected off the server's own classes.
 *
 * <p>These pin the reflect-over-repeat contract: parameter names are real names (which is what the
 * build's {@code -parameters} is for), parameter types come from the Java types, an optional
 * parameter's declared default matches the constant the convenience constructor actually uses, and
 * a node that cannot be described unambiguously is rejected rather than guessed at.
 */
class BotNodeRegistryTest {

	@Test
	void everyKnownNodeIsAnnotatedAndImplementsBotState() {
		for (Class<? extends BotState> type : BotNodeRegistry.knownNodeClasses()) {
			assertTrue(BotNodeRegistry.isNode(type), type.getName() + " is not annotated");
			assertTrue(BotState.class.isAssignableFrom(type), type.getName() + " is not a BotState");
		}
	}

	@Test
	void schemaIdsAreUnique() {
		Set<String> ids = new HashSet<String>();
		for (NodeSchema schema : BotNodeRegistry.schemas()) {
			assertTrue(ids.add(schema.id()), "duplicate node id " + schema.id());
		}
	}

	@Test
	void parameterNamesAreTheRealNamesNotArgZero() {
		for (NodeSchema schema : BotNodeRegistry.schemas()) {
			for (NodeParam param : schema.params()) {
				assertFalse(param.name().matches("arg\\d+"),
						schema.id() + " reflected " + param.name() + "; -parameters is off");
				assertTrue(param.name().matches("[A-Za-z_][A-Za-z0-9_]*"),
						schema.id() + " has a suspicious parameter name " + param.name());
			}
		}
	}

	@Test
	void parameterTypesComeFromTheJavaTypes() {
		assertEquals(Arrays.asList("destX:INT", "destY:INT", "range:INT", "stuckBudget:INT"),
				signature(registry("walk_to")));
		assertEquals(Arrays.asList("treeId:INT", "logItemId:INT", "treeX:INT", "treeY:INT"),
				signature(registry("chop_tree")));
		assertEquals(Arrays.asList("logItemId:INT"), signature(registry("bank_logs")));
		assertEquals(Arrays.asList("children:NODE_LIST"), signature(registry("sequence")));
		assertEquals(Arrays.asList("child:NODE", "count:INT"), signature(registry("repeat")));
	}

	/**
	 * Phase B's vocabulary, reflected. The arrangement nodes take children and the conditions
	 * take plain values, so this is also the check that a varargs child list is a NODE_LIST and
	 * a single child is a NODE — the distinction the editor draws a drop target from.
	 */
	@Test
	void theTreeKitNodesReflectTheirDeclaredParameters() {
		assertEquals(Arrays.asList("children:NODE_LIST"), signature(registry("selector")));
		assertEquals(Arrays.asList("children:NODE_LIST"), signature(registry("random_selector")));
		assertEquals(Arrays.asList("children:NODE_LIST"), signature(registry("parallel")));

		assertEquals(Arrays.asList("child:NODE", "attempts:INT"), signature(registry("retry")));
		assertEquals(Arrays.asList("child:NODE", "ticks:INT"), signature(registry("timeout")));
		assertEquals(Arrays.asList("ticks:INT"), signature(registry("delay")));
		assertEquals(Arrays.asList("child:NODE", "ticks:INT"), signature(registry("cooldown")));
		assertEquals(Arrays.asList("child:NODE"), signature(registry("invert")));
		assertEquals(Arrays.asList("child:NODE"), signature(registry("succeed")));
		assertEquals(Arrays.asList("child:NODE"), signature(registry("fail")));

		assertEquals(Arrays.asList("itemId:INT"), signature(registry("has_item")));
		assertEquals(Arrays.asList("x:INT", "y:INT", "range:INT"), signature(registry("within_range")));
		assertEquals(Arrays.asList("skill:INT", "level:INT"), signature(registry("skill_at_least")));
	}

	/**
	 * Phase D's nodes, reflected. Both take a {@code LocationKind}, which is why the type mapping gained
	 * {@link ParamType#KIND}: a script names a kind ({@code tree}, {@code bank}), and the editor needs a
	 * field for it that is not a free-text string.
	 */
	@Test
	void thePhaseDScriptNodesReflectTheirDeclaredParameters() {
		assertEquals(Arrays.asList("kind:KIND", "range:INT", "seed:INT"),
				signature(registry("walk_to_nearest")));
		assertEquals(Arrays.asList("kind:KIND", "itemId:INT", "range:INT", "radius:INT", "click:INT"),
				signature(registry("gather")));

		assertEquals("-1", registry("walk_to_nearest").params().get(2).defaultValue());
		assertEquals("3", registry("gather").params().get(2).defaultValue());
		assertEquals("8", registry("gather").params().get(3).defaultValue());
		assertEquals("0", registry("gather").params().get(4).defaultValue(),
				"the first click, which is what a tree and a rock both answer to");
	}

	/** A constant question has no fields for the editor to render. */
	@Test
	void aConstantConditionDeclaresNoParameters() {
		assertTrue(registry("inventory_full").params().isEmpty());
		assertTrue(registry("bank_open").params().isEmpty());
		assertTrue(registry("is_dead").params().isEmpty());
	}

	@Test
	void everyParamTypeIsReachableFromAJavaType() {
		Constructor<?> constructor = KitchenSink.class.getDeclaredConstructors()[0];
		List<ParamType> types = new ArrayList<ParamType>();
		for (Parameter parameter : constructor.getParameters()) {
			types.add(BotNodeRegistry.typeOf(KitchenSink.class, parameter));
		}
		assertEquals(Arrays.asList(ParamType.INT, ParamType.BOOLEAN, ParamType.STRING, ParamType.TILE,
				ParamType.LOCATION, ParamType.KIND, ParamType.NODE, ParamType.NODE_LIST), types);
	}

	@Test
	void requiredParamsHaveNoDefaultAndOptionalParamsDeclareOne() {
		for (NodeSchema schema : BotNodeRegistry.schemas()) {
			for (NodeParam param : schema.params()) {
				if (param.required()) {
					assertNull(param.defaultValue(),
							schema.id() + "." + param.name() + " is required but has a default");
				} else {
					assertFalse(param.defaultValue().isEmpty(),
							schema.id() + "." + param.name() + " is optional but declares no default");
				}
			}
		}
		assertEquals("40", registry("walk_to").params().get(3).defaultValue());
		assertEquals("-1", registry("repeat").params().get(1).defaultValue());
		assertEquals("3", registry("retry").params().get(1).defaultValue());
	}

	/**
	 * The optional default is declared twice — once in the annotation, once as the constant the
	 * convenience constructor delegates with — so pin them together. Without this, editing
	 * {@code DEFAULT_STUCK_BUDGET} or {@code DEFAULT_ATTEMPTS} would leave the editor showing a
	 * value the server does not use.
	 */
	@Test
	void theDeclaredDefaultMatchesTheConstantTheConvenienceConstructorUses() throws Exception {
		assertEquals(integerConstant(WalkTo.class, "DEFAULT_STUCK_BUDGET"),
				registry("walk_to").params().get(3).defaultValue());
		assertEquals(integerConstant(Retry.class, "DEFAULT_ATTEMPTS"),
				registry("retry").params().get(1).defaultValue());
		assertEquals(integerConstant(Gather.class, "DEFAULT_RANGE"),
				registry("gather").params().get(2).defaultValue());
	}

	private static String integerConstant(Class<?> owner, String name) throws Exception {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		return Integer.toString(field.getInt(null));
	}

	@Test
	void aNodeWithTwoFullyAnnotatedConstructorsIsRejectedRatherThanGuessed() {
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
				() -> BotNodeRegistry.schemaOf(Ambiguous.class));
		assertTrue(thrown.getMessage().contains("two constructors"), thrown.getMessage());
	}

	@Test
	void aNodeWithAParameterTheSchemaCannotDescribeIsRejected() {
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
				() -> BotNodeRegistry.schemaOf(Unmappable.class));
		assertTrue(thrown.getMessage().contains("ParamType"), thrown.getMessage());
	}

	@Test
	void aClassThatIsNotAnnotatedHasNoSchema() {
		assertThrows(IllegalArgumentException.class, () -> BotNodeRegistry.schemaOf(NotANode.class));
	}

	@Test
	void schemasAreSortedByIdSoTheExportIsADiffableStableFile() {
		List<NodeSchema> schemas = BotNodeRegistry.schemas();
		List<String> ids = new ArrayList<String>();
		for (NodeSchema schema : schemas) {
			ids.add(schema.id());
		}
		List<String> sorted = new ArrayList<String>(ids);
		sorted.sort(String::compareTo);
		assertEquals(sorted, ids);
		assertEquals(schemas.size(), BotNodeRegistry.byId().size());
	}

	private static NodeSchema registry(String id) {
		NodeSchema schema = BotNodeRegistry.schemaById(id);
		assertTrue(schema != null, "no node with id " + id);
		return schema;
	}

	private static List<String> signature(NodeSchema schema) {
		List<String> out = new ArrayList<String>();
		for (NodeParam param : schema.params()) {
			out.add(param.name() + ":" + param.type().name());
		}
		return out;
	}

	// ---- fixtures ------------------------------------------------------------------------

	/** No-ops so a fixture can focus on its constructor without implementing the contract. */
	abstract static class Stub implements BotState {

		@Override
		public void enter(BotContext ctx) {
		}

		@Override
		public BotStatus tick(BotContext ctx) {
			return BotStatus.SUCCESS;
		}

		@Override
		public void exit(BotContext ctx, boolean interrupted) {
		}
	}

	/** Covers every {@link ParamType} so the type mapping is proven, not assumed. */
	@BotNode(id = "kitchen_sink", category = "test", summary = "every parameter type")
	static final class KitchenSink extends Stub {

		KitchenSink(int whole, boolean flag, String text, Tile tile, Location place,
				server.game.bots.world.LocationKind kind, BotState child, BotState... children) {
		}
	}

	@BotNode(id = "ambiguous", category = "test", summary = "two schemas")
	static final class Ambiguous extends Stub {

		Ambiguous(@Param(description = "a") int a, @Param(description = "b") int b) {
		}

		Ambiguous(@Param(description = "a") int a, @Param(description = "b") int b,
				@Param(description = "c") int c) {
		}
	}

	@BotNode(id = "unmappable", category = "test", summary = "a double")
	static final class Unmappable extends Stub {

		Unmappable(@Param(description = "not a schema type") double value) {
		}
	}

	static final class NotANode extends Stub {

		NotANode(@Param(description = "still not a node") int value) {
		}
	}
}
