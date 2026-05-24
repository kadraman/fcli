plugins { id("fcli.module-conventions") }

dependencies {
    val toolRef = project.findProperty("fcliToolRef") as String
    implementation(project(toolRef))
}
