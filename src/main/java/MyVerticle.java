import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.SharedData;
import vertx.util.Utils;

import java.util.Date;

public class MyVerticle extends AbstractVerticle {

  public void start() {

    EventBus eb = vertx.eventBus();

    SharedData sd = vertx.sharedData();
    Utils.getMap(vertx, "mymap", (err, result)->{
      AsyncMap map = (AsyncMap)result;

      vertx.createHttpServer().requestHandler(req -> {
        map.put("a", new Date(), (res)->{
          System.out.println("done");
        });

        eb.send("news", new JsonObject().put("content", "12345"), (res)->{
          System.out.println(res.result().body().getClass());
        });

        req.response()
            .putHeader("content-type", "text/html")
            .end("<html><body><h1>Hello from vert.x!</h1></body></html>");
      }).listen(8080);

    });
  }

}