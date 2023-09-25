**Requirements**

* [Fiji](https://fiji.sc/)
* [ilastik](https://www.ilastik.org/) 1.3.3 or later

**Fiji site dependencies**

Add the following sites to your list of update sites in Fiji:
* _ilastik_ site
* _IJPB-plugins_ site
* _Morphology_ site

---
**How to follow an update site in Fiji**

See [here](https://imagej.net/Following_an_update_site)

**How to set up the connection between ilastik and Fiji**

* In Fiji, click on <code>Plugins > ilastik > Configure ilastik executable location</code>
* <code>Browse</code> to select the *Path to ilastik executable*
    * E.g., the typical Windows path is <code>C:\Program Files\ilastik-1.3.3post3\ilastik.exe</code> (mind the ilastik version if you are using a most recent one: <code>C:\Program Files\ilastik-[version]\ilastik.exe</code>)

![image](https://user-images.githubusercontent.com/39589980/187649952-dfea6302-d439-49a9-9e2a-d00520ecc0b2.png)

---

**Installation**

1. Start Fiji
2. Start the **ImageJ Updater** (<code>Help > Update...</code>)
3. Click on <code>Manage update sites</code>
4. Click on <code>Add update site</code>
5. A new blank row is to be created at the bottom of the update sites list
6. Type **AimSeg** in the **Name** column
7. Type **http://sites.imagej.net/AimSeg/** in the **URL** column
8. <code>Close</code> the update sites window
9. <code>Apply changes</code>
10. Restart Fiji
11. Check if <code>AimSeg</code> appears now in the <code>Plugins</code> dropdown menu (note that it will be placed at the bottom of the list)
