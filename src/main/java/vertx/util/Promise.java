package vertx.util;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Implementation of Promise interface.
 *
 * @author truelove@cyngn.com (Jeremy Truelove) 7/30/15
 */
class Promise {

	String CONTEXT_FAILURE_KEY = "failure";
	
    private List<Action> actions;

    private int pos;
    private boolean done;
    private boolean failed;
    private Vertx vertx;
    //private Callback onFailure;
    private Callback onComplete;
    //private JsonObject context;
    private Long timerId;
    private AtomicBoolean evaluated;
    private List results;
    
    // scope to the package
    public Promise(Vertx vertx) {
        this.vertx = vertx;
        pos = 0;
        done = failed = false;
        //context = new JsonObject();
        actions = new ArrayList<>();
        evaluated = new AtomicBoolean(false);
        results = new ArrayList();
    }

    public Promise eval(){
        if(actions.size() < 1) {
            throw new IllegalStateException("cannot eval an empty promise");
        }

        if(evaluated.compareAndSet(false, true)) {
            vertx.runOnContext(this::internalEval);
        } else {
            throw new IllegalStateException("You cannot eval a promise chain more than once");
        }
        return this;
    }

    public boolean isEmpty() {
        return actions.size() == 0;
    }

    /**
     * Move the promise chain to the next step in the process.
     */
    private void internalEval(Void aVoid) {
        if (!done && pos < actions.size() && !failed) {
            Action action = actions.get(pos);
            pos++;
            try {
                action.execute((err, result) -> {
                		boolean success = err == null ? true : false;
                    if (failed || done) { return; }

                    if (!success) {
                        fail(err);
                    } else {
                    		results.add(result);
                        done = pos == actions.size();
                    }

                    // scheduled the next action
                    if (!done && !failed) {
                        vertx.runOnContext(this::internalEval);
                    }

                    if (done && !failed) {
                        cleanUp();
                        // ultimate success case
                        if(onComplete != null) { onComplete.done(null, results); }
                    }
                });
            } catch (Exception ex) {
                //context.put(CONTEXT_FAILURE_KEY, ex.toString());
                fail(ex);
            }
        }
    }

    /**
     * End the processing chain due to an error condition
     */
    private void fail(Object err) {
        failed = true;
        done = true;
        cleanUp();
        if( onComplete != null){
        		onComplete.done(err, null);
        }
    }

    /**
     * Clear local objects no longer needed
     */
    private void cleanUp() {
        cancelTimer();
        actions.clear();
    }

    public Promise all(Action ... theActions) {
        return then((onResult) -> {
        		Object[] arr = new Object[theActions.length];
        		Map<Action, Integer> actionIndexMap = new HashMap<Action, Integer>();
        		
            // track the results, but execute them all in parallel vs serially
            Latch latch = new Latch(theActions.length, () -> {
            		onResult.done(null,  arr);
            });
            for(int i = 0; i < theActions.length; i++) {
            		Action action = theActions[i];
            		actionIndexMap.put(action, i);
                action.execute((err, result) -> {
                		boolean success = err == null ? true : false;
                    if(!success) {
                        onResult.done(err, null);
                    } else {
                    		arr[actionIndexMap.get(action)] = result;
                        latch.complete();
                    }
                });
            }
        });
    }
        
    public Promise allInOrder(Action... actions) {
        for (Action action : actions) { then(action); }
        return this;
    }

    public Promise then(Action action) {
        if (done) { throw new IllegalArgumentException("can't add actions to a completed chain"); }

        actions.add(action);
        return this;
    }

    public Promise done(Callback action) {
        onComplete = action;
        return this;
    }

    public Promise timeout(long time) {
        if(done) { throw new IllegalArgumentException("Can't set timer on a completed promise"); }

        if(timerId != null) {
            // if you are able to cancel it schedule another
            if(vertx.cancelTimer(timerId)) {
                timerId = vertx.setTimer(time, theTimerId -> cancel());
            }
        } else {
            timerId = vertx.setTimer(time, theTimerId -> cancel());
        }

        return this;
    }

    /**
     * Get rid of a timer that has not been fired yet.
     */
    private void cancelTimer(){
        if(timerId != null) {
            vertx.cancelTimer(timerId);
        }
    }

    /**
     * Function called when a timer is expired but the chain is not yet complete.
     */
    private void cancel() {
        timerId = null;
        if(!done) {
            //context.put(CONTEXT_FAILURE_KEY, "promise timed out");
            fail("TIMEOUT");
        }
    }

    public boolean succeeded() {
        return !failed;
    }

    public boolean completed() {
        return done;
    }    
}
