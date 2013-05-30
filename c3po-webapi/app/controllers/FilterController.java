package controllers;

import helpers.BasicGraph;
import helpers.Graph;
import helpers.BubbleGraph;
import helpers.PropertyValuesFilter;
import helpers.Statistics;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Logger;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.MapReduceOutput;
import com.petpet.c3po.analysis.mapreduce.HistogramJob;
import com.petpet.c3po.analysis.mapreduce.MapReduceJob;
import com.petpet.c3po.analysis.mapreduce.NumericAggregationJob;
import com.petpet.c3po.api.dao.Cache;
import com.petpet.c3po.api.dao.PersistenceLayer;
import com.petpet.c3po.common.Constants;
import com.petpet.c3po.datamodel.Filter;
import com.petpet.c3po.datamodel.Property;
import com.petpet.c3po.datamodel.Property.PropertyType;
import com.petpet.c3po.utils.Configurator;
import com.petpet.c3po.utils.DataHelper;

public class FilterController extends Controller {

  /**
   * Gets all selected filters and returns them to the client, so that it can
   * reconstruct the page.
   * 
   * @return
   */
  public static Result getAll() {
    Logger.debug("in method getAll(), retrieving all properties");
    List<PropertyValuesFilter> filters = new ArrayList<PropertyValuesFilter>();
    Filter filter = Application.getFilterFromSession();

    if (filter != null) {
      BasicDBObject ref = new BasicDBObject("descriminator", filter.getDescriminator());
      ref.put("collection", filter.getCollection());
      DBCursor cursor = Configurator.getDefaultConfigurator().getPersistence().find(Constants.TBL_FILTERS, ref);

      while (cursor.hasNext()) {
        Filter tmp = DataHelper.parseFilter(cursor.next());
        if (tmp.getProperty() != null && tmp.getValue() != null) {
          final Cache cache = Configurator.getDefaultConfigurator().getPersistence().getCache();
          final Property property = cache.getProperty(tmp.getProperty());
          PropertyValuesFilter f = getValues(tmp.getCollection(), property, tmp.getValue());

          f.setSelected(tmp.getValue());
          filters.add(f);
        }
      }
    }

    return ok(play.libs.Json.toJson(filters));

  }

  public static Result remove(String property) {
    Logger.debug("in method remove(String property), removing filter with property " + property);
    PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();
    Filter filter = Application.getFilterFromSession();
    BasicDBObject query = new BasicDBObject("descriminator", filter.getDescriminator());
    query.put("collection", filter.getCollection());
    query.put("property", property);

    DBCursor cursor = p.find(Constants.TBL_FILTERS, query);
    if (cursor.count() == 0) {
      Logger.debug("No filter found for property: " + property);
    } else if (cursor.count() == 1) {
      Logger.debug("Removing filter for property: " + property);
      Filter tmp = DataHelper.parseFilter(cursor.next());
      p.getDB().getCollection(Constants.TBL_FILTERS).remove(tmp.getDocument());
    } else {
      Logger.error("Something went wrong, while removing filter for property: " + property);
      throw new RuntimeException("Two many filters found for property " + property);
    }

    return ok();
  }

  public static Result add() {
    Logger.debug("in method add(), adding new filter");
    // final List<String> names = Application.getCollectionNames();
    Filter filter = Application.getFilterFromSession();

    if (filter != null) {
      final DynamicForm form = form().bindFromRequest();
      final String f = form.get("filter");
      final String v = form.get("value");
      final String t = form.get("type");
      final String a = form.get("alg");
      final String w = form.get("width");

      if (t == null || t.equals("normal")) {
        return addFromFilter(filter, f, v);
      } else if (t.equals("graph")) {
        int value = Integer.parseInt(v);

        return addFromGraph(filter, f, value, a, w);
      }
    }

    return badRequest("No filter was found in the session\n");
  }

  private static Result addFromFilter(Filter filter, String f, String v) {
    Logger.debug("in method addFromFilter(), adding new filter with property '" + f + "' and value '" + v + "'");
    PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();

    BasicDBObject ref = new BasicDBObject("descriminator", filter.getDescriminator());
    ref.put("collection", filter.getCollection());
    DBCursor cursor = Configurator.getDefaultConfigurator().getPersistence().find(Constants.TBL_FILTERS, ref);
    boolean existing = false;
    while (cursor.hasNext()) {
      Filter tmp = DataHelper.parseFilter(cursor.next());
      if (tmp.getProperty() != null && tmp.getProperty().equals(f)) {
        Logger.debug("Filter is already present, changing value");
        p.getDB().getCollection(Constants.TBL_FILTERS).remove(tmp.getDocument());

        tmp.setValue(v);
        p.insert(Constants.TBL_FILTERS, tmp.getDocument());
        existing = true;
        break;
      }
    }

    if (!existing) {
      Logger.info("Filtering based on new filter: " + filter + " " + v);
      Filter newFilter = new Filter(filter.getCollection(), f, v);
      newFilter.setDescriminator(filter.getDescriminator());
      p.insert(Constants.TBL_FILTERS, newFilter.getDocument());
    }

    return ok();

  }

  private static Result addFromGraph(Filter filter, String f, int value, String alg, String width) {
    Logger.debug("in method addFromGraph(), adding new filter with property '" + f.toString() + "' and position value '" + value
        + "'");
    Logger.info("Current filter was: " + filter.getDescriminator());
    // query histogram to check the value of the filter that was selected

    final Cache cache = Configurator.getDefaultConfigurator().getPersistence().getCache();
    final Property property = cache.getProperty(f);
    Graph graph = null;

    if (property.getType().equals(Property.PropertyType.INTEGER.toString())) {
      graph = getNumericGraph(filter, f, alg, width);
    } else {
      graph = getGraph(filter, f);
    }

    final String filtervalue = graph.getKeys().get(value);

    return addFromFilter(filter, f, filtervalue);
  }

  public static Result getValues() {
    Logger.debug("in method getValues(), retrieving values for selected property");
    final DynamicForm form = form().bindFromRequest();
    final String c = form.get("collection");
    final String p = form.get("filter");

    // get algorithm and width
    final String a = form.get("alg");
    final String w = form.get("width");

    final Cache cache = Configurator.getDefaultConfigurator().getPersistence().getCache();
    final Property property = cache.getProperty(p);
    PropertyValuesFilter f = null;
    if (property.getType().equals(PropertyType.INTEGER.toString())) {
      f = getNumericValues(c, property, a, w);
    } else {
      f = getValues(c, property, null);
    }

    return ok(play.libs.Json.toJson(f));
  }

  private static PropertyValuesFilter getValues(String c, Property p, String v) {
    Logger.debug("get property values filter for " + c + " and property " + p.getId());

    final MapReduceJob job = new HistogramJob(c, p.getId());

    if (p.getType().equals(PropertyType.INTEGER.toString())) {
      // int width = (v == null) ? 10 : HistogramJob.inferBinWidth(v);
      int width = HistogramJob.inferBinWidth(v);

      HashMap<String, String> config = new HashMap<String, String>();
      config.put("bin_width", width + "");
      job.setConfig(config);
    }

    final MapReduceOutput output = job.execute();
    final List<String> keys = new ArrayList<String>();
    final List<String> values = new ArrayList<String>();

    if (p.getType().equals(PropertyType.INTEGER.toString())) {
      // int width = (v == null) ? 10 : HistogramJob.inferBinWidth(v);
      int width = HistogramJob.inferBinWidth(v);

      calculateNumericHistogramResults(output, keys, values, width);
    } else {
      calculateHistogramResults(output, keys, values);
    }

    PropertyValuesFilter f = new PropertyValuesFilter();
    f.setProperty(p.getId());
    f.setType(p.getType());
    f.setValues(keys); // this is not a mistake.
    f.setSelected(v);

    return f;
  }

  private static PropertyValuesFilter getNumericValues(String c, Property p, String alg, String width) {
    Filter filter = Application.getFilterFromSession();
    Graph graph = null;

    if (alg.equals("fixed")) {
      int w = Integer.parseInt(width);
      graph = getFixedWidthHistogram(filter, p.getId(), w);
    } else if (alg.equals("sqrt")) {
      graph = getSquareRootHistogram(filter, p.getId());
    } else if (alg.equals("sturge")) {
      graph = getSturgesHistogramm(filter, p.getId());
    }

    graph.sort();
//
//    if (graph.getKeys().size() > 100) {
//      graph.cutLongTail();
//    }
    
    PropertyValuesFilter f = new PropertyValuesFilter();
    f.setProperty(p.getId());
    f.setType(p.getType());
    f.setValues(graph.getKeys()); // this is not a mistake.

    return f;
  }

  public static BasicGraph getGraph(String property) {
    Filter filter = Application.getFilterFromSession();

    DynamicForm form = form().bindFromRequest();
    String alg = form.get("alg");
    BasicGraph g = null;
    String bubbleProperty = form.get("bubbleproperty");
    
    if (bubbleProperty == null) {
	    if (alg == null) {
	      g = getOrdinalGraph(filter, property);
	    } else {
	      g = getNumericGraph(filter, property, form.get("alg"), form.get("width"));
	    }
    } else {
    	Graph g1 = null;
    	Graph g2 = null;
    	
    	String alg1 = form.get("alg1");
    	if (alg1 == null) {
    		g1 = getOrdinalGraph(filter, form.get("property1"));
    	} else {
  	      	g1 = getNumericGraph(filter, form.get("property1"), alg1, form.get("width1"));
    	}
    	
    	String alg2 = form.get("alg2");
    	if (alg2 == null) {
    		g2 = getOrdinalGraph(filter, form.get("property2"));
    	} else {
    		g2 = getNumericGraph(filter, form.get("property2"), alg2, form.get("width2"));
    	}
    	
    	// join g1 and g2 together to g
    	g = joinGraphs(g1, g2);
    }

    if (g != null) {
      g.sort();

      if (g.getKeys().size() > 100) {
        g.cutLongTail();
      }
    }
    return g;
  }
  
  /**
   * Joins two Graphs to a BubbleGraph
   */
  public static BasicGraph joinGraphs(Graph g1, Graph g2) {
	  final String property = g1.getProperty() +"_versus_"+ g2.getProperty();
	  final List<Double[]> keys = new ArrayList<Double[]>();
	  final List<Object[]> values = new ArrayList<Object[]>();
	  final BubbleGraph g = new BubbleGraph(property, keys, values);
	  
	  for (int i = 0; i < g1.getKeys().size(); i++) {
		  for (int j = 0; j < g2.getKeys().size(); j++) {
			  // add keys
			  Double[] key = {(double) i, (double) j};
			  keys.add(key);
			  
			  // add values
			  // FIXME: 5 should be a value
			  Object[] value = {(double) i, (double) j, Double.parseDouble(g1.getValues().get(i)) * Double.parseDouble(g2.getValues().get(j)), g1.getKeys().get(i) +" (# of "+ g2.getKeys().get(j) +")"};
			  values.add(value);
		  }
	  }
	  
	  return g;
  }
  
  public static Graph getGraph(String collection, String property) {
    final PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();
    final List<String> keys = new ArrayList<String>();
    final List<String> values = new ArrayList<String>();
    final Graph result = new Graph(property, keys, values);

    DBCollection dbc = p.getDB().getCollection("histogram_" + collection + "_" + property);

    if (dbc.find().count() == 0) {
      final MapReduceJob job = new HistogramJob(collection, property);
      final MapReduceOutput output = job.execute();

      calculateHistogramResults(output, keys, values);

    } else {
      DBCursor cursor = dbc.find();
      while (cursor.hasNext()) {
        BasicDBObject dbo = (BasicDBObject) cursor.next();
        parseHistogram(dbo, keys, values);
      }
    }

    result.sort();

    if (result.getKeys().size() > 100) {
      result.cutLongTail();
    }

    return result;
  }

  public static Graph getGraph(Filter filter, String property) {
    final List<String> keys = new ArrayList<String>();
    final List<String> values = new ArrayList<String>();
    final Graph result = new Graph(property, keys, values);
    final BasicDBObject query = Application.getFilterQuery(filter);
    final MapReduceJob job = new HistogramJob(filter.getCollection(), property, query);

    final Cache cache = Configurator.getDefaultConfigurator().getPersistence().getCache();
    final Property p = cache.getProperty(property);
    long width = -1;

    if (p.getType().equals(PropertyType.INTEGER.toString())) {
      DBObject range = (DBObject) query.get("metadata." + property + ".value");
      
      Long low = (Long) range.get("$gte");
      Long high = (Long) range.get("$lte");
      
      width = high - low + 1; //because of lte/gte

      HashMap<String, String> config = new HashMap<String, String>();
      config.put("bin_width", width + "");
      job.setConfig(config);

    }

    final MapReduceOutput output = job.execute();
    if (p.getType().equals(PropertyType.INTEGER.toString())) {
      calculateNumericHistogramResults(output, keys, values, width);
    } else {
      calculateHistogramResults(output, keys, values);
    }
    
    result.sort();

    if (result.getKeys().size() > 100) {
      result.cutLongTail();
    }
    
    return result;
  }

  public static Statistics getCollectionStatistics(Filter filter) {
    final NumericAggregationJob job = new NumericAggregationJob(filter.getCollection(), "size");
    final BasicDBObject query = Application.getFilterQuery(filter);
    job.setFilterquery(query);

    final MapReduceOutput output = job.execute();
    final List<BasicDBObject> results = (List<BasicDBObject>) output.getCommandResult().get("results");
    BasicDBObject aggregation = null;

    if (!results.isEmpty()) {
      aggregation = (BasicDBObject) results.get(0).get("value");
    }

    return getStatisticsFromResult(aggregation);
  }

  public static Statistics getCollectionStatistics(String name) {
    final PersistenceLayer pl = Configurator.getDefaultConfigurator().getPersistence();
    BasicDBObject aggregation = null;

    DBCollection collection = pl.getDB().getCollection("statistics_" + name);
    if (collection.find().count() != 0) {
      aggregation = (BasicDBObject) collection.findOne().get("value");
    }

    if (aggregation == null) {
      final NumericAggregationJob job = new NumericAggregationJob(name, "size");
      job.setType(OutputType.REPLACE);
      job.setOutputCollection("statistics_" + name);
      final MapReduceOutput output = job.execute();

      if (output != null) {
        aggregation = (BasicDBObject) collection.findOne().get("value");
      }
    }

    return getStatisticsFromResult(aggregation);
  }

  public static Statistics getStatisticsFromResult(BasicDBObject aggregation) {
    if (aggregation == null)
      return null;

    final DecimalFormat df = new DecimalFormat("#.##");
    Statistics stats = new Statistics();
    stats.setCount(aggregation.getInt("count") + " objects");
    stats.setSize(df.format(aggregation.getLong("sum") / 1024D / 1024) + " MB");
    stats.setAvg(df.format(aggregation.getDouble("avg") / 1024 / 1024) + " MB");
    stats.setMin(aggregation.getLong("min") + " B");
    stats.setMax(df.format(aggregation.getLong("max") / 1024D / 1024) + " MB");
    stats.setSd(df.format(aggregation.getDouble("stddev") / 1024 / 1024) + " MB");
    stats.setVar(df.format(aggregation.getDouble("variance") / 1024 / 1024 / 1024 / 1024) + " MB");
    // because of sd^2
    return stats;
  }

  private static Graph getOrdinalGraph(Filter filter, String property) {
    Graph g = null;
    if (filter != null) {
      BasicDBObject ref = new BasicDBObject("descriminator", filter.getDescriminator());
      DBCursor cursor = Configurator.getDefaultConfigurator().getPersistence().find(Constants.TBL_FILTERS, ref);
      if (cursor.count() == 1) { // only root filter
        g = FilterController.getGraph(filter.getCollection(), property);
      } else {
        g = FilterController.getGraph(filter, property);
      }

    }

    return g;
  }

  private static Graph getNumericGraph(Filter filter, String property, String alg, String w) {

    // TODO find number of elements based on filter...
    // calculate bins...
    // find classes based on number of bins...
    // map reduce this property based on the classes...
    Graph g = null;
    if (alg.equals("fixed")) {
      int width = 50;
      try {
        width = Integer.parseInt(w);
      } catch (NumberFormatException e) {
        Logger.warn("Not a number, using default bin width: 50");
      }

      g = getFixedWidthHistogram(filter, property, width);
      g.getOptions().put("width", w);

    } else if (alg.equals("sturge")) {
      // bins = log2 n + 1
      g = getSturgesHistogramm(filter, property);
    } else if (alg.equals("sqrt")) {
      // bins = sqrt(n);
      g = getSquareRootHistogram(filter, property);
    }

    g.getOptions().put("type", PropertyType.INTEGER.toString());
    g.getOptions().put("alg", alg);

    
    g.sort();
    
    return g;
  }

  private static Graph getFixedWidthHistogram(Filter filter, String property, int width) {
    BasicDBObject query = Application.getFilterQuery(filter);
    MapReduceJob job = new NumericAggregationJob(filter.getCollection(), property);
    job.setFilterquery(query);

    MapReduceOutput output = job.execute();
    List<BasicDBObject> results = (List<BasicDBObject>) output.getCommandResult().get("results");
    Graph g = null;
    if (!results.isEmpty()) {
      BasicDBObject aggregation = (BasicDBObject) results.get(0).get("value");
      long min = aggregation.getLong("min");
      long max = aggregation.getLong("max");

      int bins = (int) ((max - min) / width);
      Map<String, String> config = new HashMap<String, String>();
      config.put("bin_width", width + "");

      job = new HistogramJob(filter.getCollection(), property);
      job.setFilterquery(query);
      job.setConfig(config);
      output = job.execute();
      results = (List<BasicDBObject>) output.getCommandResult().get("results");
      List<String> keys = new ArrayList<String>();
      List<String> values = new ArrayList<String>();

      calculateNumericHistogramResults(output, keys, values, width);

      g = new Graph(property, keys, values);
    }

    return g;

  }

  private static Graph getSturgesHistogramm(Filter f, String property) {
    BasicDBObject query = Application.getFilterQuery(f);
    DBCursor cursor = Configurator.getDefaultConfigurator().getPersistence().find(Constants.TBL_ELEMENTS, query);
    int n = cursor.size();
    int bins = (int) ((Math.log(n) / Math.log(2)) + 1);
    MapReduceJob job = new NumericAggregationJob(f.getCollection(), property);
    job.setFilterquery(query);

    MapReduceOutput output = job.execute();
    List<BasicDBObject> results = (List<BasicDBObject>) output.getCommandResult().get("results");
    Graph g = null;
    if (!results.isEmpty()) {
      BasicDBObject aggregation = (BasicDBObject) results.get(0).get("value");
      long max = aggregation.getLong("max");
      int width = (int) (max / bins);
      Map<String, String> config = new HashMap<String, String>();
      config.put("bin_width", width + "");

      job = new HistogramJob(f.getCollection(), property);
      job.setFilterquery(query);
      job.setConfig(config);
      output = job.execute();
      List<String> keys = new ArrayList<String>();
      List<String> values = new ArrayList<String>();

      calculateNumericHistogramResults(output, keys, values, width);

      g = new Graph(property, keys, values);
    }

    return g;
  }

  private static Graph getSquareRootHistogram(Filter f, String property) {
    BasicDBObject query = Application.getFilterQuery(f);
    DBCursor cursor = Configurator.getDefaultConfigurator().getPersistence().find(Constants.TBL_ELEMENTS, query);
    int n = cursor.size();
    int bins = (int) Math.sqrt(n);
    MapReduceJob job = new NumericAggregationJob(f.getCollection(), property);
    job.setFilterquery(query);

    MapReduceOutput output = job.execute();
    List<BasicDBObject> results = (List<BasicDBObject>) output.getCommandResult().get("results");
    Graph g = null;
    if (!results.isEmpty()) {
      BasicDBObject aggregation = (BasicDBObject) results.get(0).get("value");
      long max = aggregation.getLong("max");
      int width = (int) (max / bins);
      Map<String, String> config = new HashMap<String, String>();
      config.put("bin_width", width + "");

      job = new HistogramJob(f.getCollection(), property);
      job.setFilterquery(query);
      job.setConfig(config);
      output = job.execute();
      List<String> keys = new ArrayList<String>();
      List<String> values = new ArrayList<String>();

      calculateNumericHistogramResults(output, keys, values, width);

      g = new Graph(property, keys, values);
    }

    return g;
  }

  private static void calculateHistogramResults(MapReduceOutput output, List<String> keys, List<String> values) {
    final List<BasicDBObject> jobresults = (List<BasicDBObject>) output.getCommandResult().get("results");
    for (final BasicDBObject dbo : jobresults) {
      parseHistogram(dbo, keys, values);
    }
  }

  private static void calculateNumericHistogramResults(MapReduceOutput output, List<String> keys, List<String> values,
      long width) {
    List<BasicDBObject> results = (List<BasicDBObject>) output.getCommandResult().get("results");
    for (BasicDBObject obj : results) {
      
      String id = obj.getString("_id");

      if (!id.equals("Unknown") && !id.equals("Conflicted")) {
        long low = (int) Double.parseDouble(id) * width;
        long high = low + width - 1;
        keys.add(low + " - " + high);
      } 
      values.add(obj.getString("value"));
    }
  }

  private static void parseHistogram(BasicDBObject dbo, List<String> keys, List<String> values) {
    String key = dbo.getString("_id");
    if (key.endsWith(".0")) {
      key = key.substring(0, key.length() - 2);
    }
    keys.add(key);
    values.add(dbo.getString("value"));
  }
}
