def config = [
    name: "tim",
    directory: "/blah"
]
def options = [ config: config ]

vertx.deployVerticle("MyVerticle")
vertx.deployVerticle("src/main/groovy/server.groovy")
