package vertx.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.Vertx;

public class Async {
    public static void paralell(Action[] theActions, Callback callback) {
    		BooleanHolder called = new BooleanHolder();
		Object[] arr = new Object[theActions.length];
		Map<Action, Integer> actionIndexMap = new HashMap<Action, Integer>();
		
	    Latch latch = new Latch(theActions.length, () -> {
	    		if( !called.value) {
	    			called.value = true;
	    			callback.done(null, Arrays.asList(arr));
	    		}
	    });
	    for(int i = 0; i < theActions.length; i++) {
	    		Action action = theActions[i];
	    		actionIndexMap.put(action, i);
	        action.execute((err, result) -> {
	        		boolean success = (err == null ? true : false);
	            if(!success) {
	            		if(!called.value){
	            			called.value = true;
	            			callback.done(err, Arrays.asList(arr));	            			
	            		}
	            } else {
	            		arr[actionIndexMap.get(action)] = result;
	                latch.complete();
	            }
	        });
	    }    	
    }
    
    public static void series(Vertx vertx, Action[] actions, Callback callback){
    		Promise p = new Promise(vertx);
    		
    		for(Action a : actions)
    			p.then(a);
    		
    		p.done((err, results)-> {
    			if(err != null)
    				callback.done(err, null);
    			else
    				callback.done(null, results);
    		});
    		p.eval();
    }
    
    public static void eachParalell(Vertx vertx, List tasks, TaskAction action, Callback callback){
    		if(tasks == null || tasks.size() == 0){
    			callback.done(null, new ArrayList());
    			return;
    		}
    		
		Object[] arr = new Object[tasks.size()];
		Map<Object, Integer> taskIndexMap = new HashMap<Object, Integer>();
		
	    Latch latch = new Latch(tasks.size(), () -> {
	    		callback.done(null, Arrays.asList(arr));
	    });
	    for(int i = 0; i < tasks.size(); i++) {
	    		Object task = tasks.get(i);
	    		taskIndexMap.put(task, i);
	        action.execute(task, i, (err, result) -> {
	        		boolean success = err == null ? true : false;
	            if(!success) {
	                callback.done(err, null);
	            } else {
	            		arr[taskIndexMap.get(task)] = result;
	                latch.complete();
	            }
	        });
	    }    	    		
    }
    
    public static void eachSeries(Vertx vertx, List tasks, TaskAction action, Callback callback){
    		new TaskPromise(vertx, tasks, action).done(callback).eval();
    }
}

class BooleanHolder {
	boolean value = false;
}
