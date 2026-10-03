package server.game.players.packets;

import server.game.players.Client;

/**
 * Change appearance
 **/

public class ChangeAppearance implements PacketType {

  @Override
  public void processPacket(Client c, int packetType, int packetSize) {
    int gender = c.getInStream().readSignedByte();
    int head = c.getInStream().readSignedByte();
    int jaw = c.getInStream().readSignedByte();
    int torso = c.getInStream().readSignedByte();
    int arms = c.getInStream().readSignedByte();
    int hands = c.getInStream().readSignedByte();
    int legs = c.getInStream().readSignedByte();
    int feet = c.getInStream().readSignedByte();
    int hairColour = c.getInStream().readSignedByte();
    int torsoColour = c.getInStream().readSignedByte();
    int legsColour = c.getInStream().readSignedByte();
    int feetColour = c.getInStream().readSignedByte();
    int skinColour = c.getInStream().readSignedByte();

    if (c.appearance.canChangeAppearance) { 
      c.appearance.playerAppearance[0] = gender; // gender
      c.appearance.playerAppearance[6] = feet; // feet
      c.appearance.playerAppearance[7] = jaw; // beard
      c.appearance.playerAppearance[8] = hairColour; // hair colour
      c.appearance.playerAppearance[9] = torsoColour; // torso colour
      c.appearance.playerAppearance[10] = legsColour; // legs colour
      c.appearance.playerAppearance[11] = feetColour; // feet colour
      c.appearance.playerAppearance[12] = skinColour; // skin colour
      if(head < 0) // head
      c.appearance.playerAppearance[1] = head + 256;
      else
      c.appearance.playerAppearance[1] = head;
      if(torso < 0)
      c.appearance.playerAppearance[2] = torso + 256;
      else
      c.appearance.playerAppearance[2] = torso;
      if(arms < 0)
      c.appearance.playerAppearance[3] = arms + 256;
      else
      c.appearance.playerAppearance[3] = arms;
      if(hands < 0)
      c.appearance.playerAppearance[4] = hands + 256;
      else
      c.appearance.playerAppearance[4] = hands;
      if(legs < 0)
      c.appearance.playerAppearance[5] = legs + 256;
      else
      c.appearance.playerAppearance[5] = legs;

      c.getPA().removeAllWindows();
      c.getPA().requestUpdates();
      c.appearance.canChangeAppearance = false;
    }  
  }  
}