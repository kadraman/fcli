plugins { id("fcli.module-conventions") }

dependencies {
    val commonActionRef = project.findProperty("fcliCommonActionRef") as String
    val commonToolRef = project.findProperty("fcliCommonToolRef") as String
    val fodRef = project.findProperty("fcliFoDRef") as String
    val sscRef = project.findProperty("fcliSSCRef") as String
    implementation(project(commonActionRef))
    implementation(project(commonToolRef))
    implementation(project(fodRef))
    implementation(project(sscRef))
}
