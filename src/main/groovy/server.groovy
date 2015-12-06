import io.vertx.groovy.core.shareddata.AsyncMap
import io.vertx.groovy.core.shareddata.SharedData
import vertx.util.Action
import vertx.util.Async
import vertx.util.Utils;


def sd = vertx.sharedData()
AsyncMap map = null;

sd.getClusterWideMap("mymap", { res ->
  if (res.succeeded()) {
    map = res.result();
  } else {
    // Something went wrong!
  }
})

vertx.createHttpServer().requestHandler({ req ->

  map.get("a", {res->
    println res.succeeded();
    println res.result();
    println res.result().getClass();
  })

  req.response()
    .putHeader("content-type", "text/html")
    .end("""
  <html><body>
  <h1>Hello from vert.x</h1>
  </body></html>
  """)
}).listen(8081)

def eb = vertx.eventBus()
eb.consumer("news", { message ->
  println message.body().getClass()
  println("I have received a message: ${message.body()}")
  message.reply([a:1])
})

println 'listen 8081'
