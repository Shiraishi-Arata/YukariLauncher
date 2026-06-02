evaluationDependsOnChildren()

tasks.register("build") {
    dependsOn(subprojects.map { it.tasks.named("build") })
}

tasks.register("packageZip") {
    dependsOn(subprojects.map { it.tasks.named("packageZip") })
}
