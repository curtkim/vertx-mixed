import java.io.Serializable;

/**
 * Created by curtkim on 2015. 12. 6..
 */
public class MyValue implements Serializable{
  public String name;

  public MyValue(String name){
    this.name = name;
  }
}
