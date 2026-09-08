New Features:

Secondary Groups (if enabled, will search secondary groups of members for groups).

Hide Members (you can now choose to "hide" members out of groups).

2 layouts: New Style, works similar to how IPB 3.3+ renders its team moderator page with pagination, Old Style, how custom moderator team page works now, creates individual blocks for each group.

add a member to a group.(it will be a pseudo add like changing the group name, they will not inherit the permissions from the group they are displayed in).

on the display page, you can now choose to show either the user group or their title, and either their join date or last active time.

added settings module

new administrator restrictions available.


changes:

Overview: has gotten a slight make over, now no more annoying "update available" block, it has now been move to "General information", it will display the current version that is available and the one that is installed.

Display Groups: big changes here. There is no longer 2 blocks showing displayed groups and select groups, its one block now, with groups not being displayed appearing below displayed groups.

Settings: added a new module for the settings so you wont have to go searching thru system settings for them.
Uses custom template for all versions of IPB, to accommodate for the changes of displaying either group or title/join date or last active time.


To Add Groups:

1. Go to "Other Apps -> Custom Moderator Team Page - > Groups".

2. in the "select Groups To Display" block, a green check mark means the group is being shown, a red x means its not displayed.

3. click on the red x to display groups, click on the green check mark removes the group.



To Change Groups Name:

1. Add the group by click on the red check mark.

2. Then in the "Displayed Name" column, click on the name.

3. change the group name, then hit the [+], if you want to cancel your change click the [x]. (if you want it to go back to normal, just save it as an empty field, and it will display the default group name, this option only works in the old layout mode).

To Hide Members in groups:

1. add the group.

2. next to the member count in the "Members" column, there will be a [+], click on that, it will dispaly the names of all the members in that group (including any members with secondary groups set to this group, if this option is enabled).

3. click the green check mark next to the name, it will replace it with a red x, and the member will now not be displayed.



Add members to group they do not belong too:

1. add the group, refresh page.

2. in the add member group block above "select groups to display", enter the users name you wish to add (its tied into the IPB autocomplete function to find members), either select the member for the list, or type out their name. 

3. select the group you wish to add them too (only groups that are being displayed, will be shown here).

4. click "Add To Group".

5. if you wish to remove the member from the group, locate the group you added them to, click on the [+], find them in the list, and click the green check mark, this will remove them from the group and they will no longer display on the front page.


Settings:

General Settings:

Search Secondary Group, if this is enabled, it will also try to locate all the members that belong to a group, if it is marked as their secondary group.


New Layout Settings:

New Layout: if yes is selected, it will use the new layout that ipb 3.3.3+ uses, where it displays all the members in one block, using pagination. If no is selected, it will use the old style where it breaks up each group into blocks of their own.

New Layout Title: if yes is selected above, you can rename the single block it creates to what you desire it to be named.

Per Page: with the pagination, you can determine how many members it shows, before it starts up paging cmtp.


Template Settings:

Use Member Title: instead of displaying the members group beside their name in cmtp, you can choose to display what their title is instead.

Show Join Date: instead of displaying the members last active time, you can choose to display the date they joined instead.
