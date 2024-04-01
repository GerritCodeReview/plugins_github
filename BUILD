java_plugin(
    name = "lombok_plugin",
    generates_api = True,
    processor_class = "lombok.launch.AnnotationProcessorHider$AnnotationProcessor",
    visibility = ["//visibility:public"],
    deps = ["@lombok//jar"],
)

java_library(
    name = "lombok",
    exported_plugins = [":lombok_plugin"],
    neverlink = True,
    visibility = ["//visibility:public"],
    exports = ["@lombok//jar"],
)
