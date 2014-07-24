package water.api;

import hex.glm.GLMModel;
import water.*;
import water.fvec.Frame;
import water.fvec.Vec;
import water.util.RString;

public class GLMPredict extends Request2 {
  static final int API_WEAVER = 1; // This file has auto-gen'd doc & json fields
  static public DocGen.FieldDoc[] DOC_FIELDS; // Initialized from Auto-Gen code.

  @API(help = "Model key", required = true, filter = Default.class)
  public Key modelKey; // Type to Model when retired OldModel

  @API(help="lambda",required=false,filter=Default.class)
  double lambda = Double.NaN;

  @API(help = "Data frame", required = true, filter = Default.class)
  public Frame data;

  @API(help = "Prediction", filter = Default.class)
  public Key prediction;

  public static String link(Key k, double lambda, String content) {
    RString rs = new RString("<a href='GLMPredict.query?model=%$key&lambda=%lambda'>%content</a>");
    rs.replace("key", k.toString());
    rs.replace("lambda",lambda);
    rs.replace("content", content);
    return rs.toString();
  }

  @Override protected Response serve() {
    try {
      if( modelKey == null )
        throw new IllegalArgumentException("Model is required to perform validation!");
      final Key predictionKey = ( prediction == null )?Key.make("__Prediction_" + Key.make()):prediction;
      GLMModel.GetScoringModelTask task = new GLMModel.GetScoringModelTask(null,null,modelKey,lambda);
      H2O.submitTask(task);
      task.get();
      GLMModel model= task._res;
      // Create a new random key
      if ( prediction == null )
        prediction = Key.make("__Prediction_" + Key.make());
      Frame fr = new Frame(prediction,new String[0],new Vec[0]).delete_and_lock(null);
      if( model instanceof Model )
        fr = ((   Model)model).score(data);
      fr = new Frame(prediction,fr._names,fr.vecs()); // Jam in the frame key
      fr.unlock(null);
      return Inspect2.redirect(this, prediction.toString());
    } catch( Throwable t ) {
      return Response.error(t);
    }
  }
}