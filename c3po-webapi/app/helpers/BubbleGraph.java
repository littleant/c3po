package helpers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A graph for bubble charts.
 * Consists of three values x,y,r but the axis can only be numbers.
 * Thus every value is encoded in the label of the bubble.
 * 
 * @author anty
 */
public class BubbleGraph implements BasicGraph {

  private String property;
  private Map<String, String> options;
  private List<Double[]> keys;
  private List<Object[]> values;

  public BubbleGraph() {
	  this.setOptions(new HashMap<String, String>());
  }

  public BubbleGraph(String p, List<Double[]> keys, List<Object[]> values) {
	  this();
	  this.property = p;
	  this.keys = keys;
	  this.values = values;
  }

  public List<Double[]> getKeys() {
    return keys;
  }

  public void setKeys(List<Double[]> keys) {
    this.keys = keys;
  }

  public List<Object[]> getValues() {
    return values;
  }

  public void setValues(List<Object[]> values) {
    this.values = values;
  }

  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public void convertToPercentage() {
	  // FIXME
//    double sum = 0;
//
//    for (String s : values) {
//      sum += Double.parseDouble(s);
//    }
//
//    List<String> res = new ArrayList<String>();
//    for (String s : values) {
//      final DecimalFormat df = new DecimalFormat("#.##");
//      double d = ((Double.parseDouble(s) / sum) * 100);
//      res.add(df.format(d) + "");
//    }
//
//    this.values = res;
  }

  public void sort() {
	  // FIXME
//    List<String> k = new ArrayList<String>();
//    List<String> v = new ArrayList<String>();
//    int target = values.size();
//    while (v.size() != target) {
//      double max = -1;
//      int pos = 0;
//      for (int i = 0; i < values.size(); i++) {
//        double current = Double.parseDouble(values.get(i));
//        if (current > max) {
//          max = current;
//          pos = i;
//        }
//      }
//      String val = values.remove(pos);
//        
//      if (val.endsWith(".0")) {
//        val = val.substring(0, val.length() - 2);
//      }
//      
//      v.add(val);
//      k.add(keys.remove(pos));
//    }
//
//    this.keys = k;
//    this.values = v;
  }
  
  public void cutLongTail() {
	// TODO: show the "rest" instead of just removing it!
    List<Double[]> k = new ArrayList<Double[]>();
    List<Object[]> v = new ArrayList<Object[]>();
        
    double sum = 0D;
    
    for (Object[] value : this.values) {
      sum += (Double) value[2];
    }
    
    int cut = (int)(sum * 0.005);
    
    for (int i = 0; i < this.values.size(); i++) {
      Double tmp = (Double) values.get(i)[2];
      if (tmp > cut) {
        k.add(keys.get(i));
        v.add(values.get(i));
      }
    }
    
    this.keys = k;
    this.values = v;
  }

  public Map<String, String> getOptions() {
    return options;
  }

  public void setOptions(Map<String, String> options) {
    this.options = options;
    this.options.put("diagramType", "Bubble");
  }

}