package edu.berkeley.cs186.database.query;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import edu.berkeley.cs186.database.Database;
import edu.berkeley.cs186.database.DatabaseException;
import edu.berkeley.cs186.database.TestUtils;
import edu.berkeley.cs186.database.StudentTestP4;
import edu.berkeley.cs186.database.databox.BoolDataBox;
import edu.berkeley.cs186.database.databox.DataBox;
import edu.berkeley.cs186.database.databox.FloatDataBox;
import edu.berkeley.cs186.database.databox.IntDataBox;
import edu.berkeley.cs186.database.databox.StringDataBox;
import edu.berkeley.cs186.database.table.MarkerRecord;
import edu.berkeley.cs186.database.table.Record;
import edu.berkeley.cs186.database.table.Schema;
import edu.berkeley.cs186.database.table.stats.StringHistogram;

import static org.junit.Assert.*;

public class TestOptimalQueryPlan {
  private Database database;
  private Random random = new Random();
  private String alphabet = StringHistogram.alphaNumeric;
  private String defaulTableName = "testAllTypes";
  private int defaultNumRecords = 1000;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Before
  public void setUp() throws DatabaseException, IOException {
    File tempDir = tempFolder.newFolder("db");
    this.database = new Database(tempDir.getAbsolutePath());
    this.database.deleteAllTables();
    this.database.createTable(TestUtils.createSchemaWithAllTypes(), this.defaulTableName);
    Database.Transaction transaction = this.database.beginTransaction();

    // by default, create 100 records
    for (int i = 0; i < this.defaultNumRecords; i++) {
      // generate a random record
      IntDataBox intValue = new IntDataBox(i);
      FloatDataBox floatValue = new FloatDataBox(this.random.nextFloat());
      BoolDataBox boolValue = new BoolDataBox(this.random.nextBoolean());
      String stringValue = "";

      for (int j = 0 ; j < 5; j++) {
        int randomIndex = Math.abs(this.random.nextInt() % alphabet.length());
        stringValue += alphabet.substring(randomIndex, randomIndex + 1);
      }

      List<DataBox> values = new ArrayList<DataBox>();
      values.add(boolValue);
      values.add(intValue);
      values.add(new StringDataBox(stringValue, 5));
      values.add(floatValue);

      transaction.addRecord("testAllTypes", values);
    }

    transaction.end();
  }

  /**
   * Test sample, do not modify.
   */
  @Test
  @Category(StudentTestP4.class)
  public void testSample() {
    assertEquals(true, true); // Do not actually write a test like this!
  }

  @Test
  @Category(StudentTestP4.class)
  public void testSimpleSelectLessThanEquals() throws DatabaseException, QueryPlanException {
    Database.Transaction transaction = this.database.beginTransaction();
    QueryPlan queryPlan = transaction.query(this.defaulTableName);

    queryPlan.select("int", QueryPlan.PredicateOperator.LESS_THAN_EQUALS, new IntDataBox(2));

    Iterator<Record> outputIterator = queryPlan.executeOptimal();

    while (outputIterator.hasNext()) {
      Record record = outputIterator.next();

      assertTrue(record.getValues().get(1).getInt() <= 2);
    }

    QueryOperator finalOperator = queryPlan.getFinalOperator();
    String tree = "type: SELECT\n" +
            "column: testAllTypes.int\n" +
            "operator: LESS_THAN_EQUALS\n" +
            "value: 2\n" +
            "\ttype: SEQSCAN\n" +
            "\ttable: testAllTypes";
    assertEquals(tree, finalOperator.toString());

    transaction.end();
  }

  @Test
  @Category(StudentTestP4.class)
  public void testSimpleSelectEquals() throws DatabaseException, QueryPlanException {
    Database.Transaction transaction = this.database.beginTransaction();
    QueryPlan queryPlan = transaction.query(this.defaulTableName);

    queryPlan.select("int", QueryPlan.PredicateOperator.EQUALS, new IntDataBox(10));

    Iterator<Record> outputIterator = queryPlan.executeOptimal();

    while (outputIterator.hasNext()) {
      Record record = outputIterator.next();

      assertTrue(record.getValues().get(1).getInt() == 10);
    }

    QueryOperator finalOperator = queryPlan.getFinalOperator();
    String tree = "type: SELECT\n" +
            "column: testAllTypes.int\n" +
            "operator: EQUALS\n" +
            "value: 10\n" +
            "\ttype: SEQSCAN\n" +
            "\ttable: testAllTypes";
    assertEquals(tree, finalOperator.toString());

    transaction.end();

  }

  @Test
  @Category(StudentTestP4.class)
  public void testSimpleProjectNotEquals() throws QueryPlanException, DatabaseException {
    Database.Transaction transaction = this.database.beginTransaction();
    QueryPlan queryPlan = transaction.query(this.defaulTableName);

    queryPlan.select("int", QueryPlan.PredicateOperator.NOT_EQUALS, new IntDataBox(50));

    Iterator<Record> outputIterator = queryPlan.executeOptimal();

    while (outputIterator.hasNext()) {
      Record record = outputIterator.next();

      assertTrue(record.getValues().get(1).getInt() != 50);
    }

    QueryOperator finalOperator = queryPlan.getFinalOperator();
    String tree = "type: SELECT\n" +
            "column: testAllTypes.int\n" +
            "operator: NOT_EQUALS\n" +
            "value: 50\n" +
            "\ttype: SEQSCAN\n" +
            "\ttable: testAllTypes";
    assertEquals(tree, finalOperator.toString());

    transaction.end();

  }

  @Test
  @Category(StudentTestP4.class)
  public void testSimpleProjectFloat() throws QueryPlanException, DatabaseException {
    Database.Transaction transaction = this.database.beginTransaction();
    QueryPlan queryPlan = transaction.query(this.defaulTableName);

    List<String> columnNames = new ArrayList<String>();
    columnNames.add("int");
    columnNames.add("float");

    queryPlan.project(columnNames);
    Iterator<Record> outputIterator = queryPlan.executeOptimal();

    int count = 0;
    while (outputIterator.hasNext()) {
      Record record = outputIterator.next();
      assertTrue(record.getValues().get(0) instanceof IntDataBox);
      assertTrue(record.getValues().get(1) instanceof FloatDataBox);

      count++;
    }

    assertEquals(this.defaultNumRecords, count);

    QueryOperator finalOperator = queryPlan.getFinalOperator();
    String tree = "type: PROJECT\n" +
            "columns: [int, float]\n" +
            "\ttype: SEQSCAN\n" +
            "\ttable: testAllTypes";
    assertEquals(tree, finalOperator.toString());

    transaction.end();
  }

  @Test
  @Category(StudentTestP4.class)
  public void testProjectEquals() throws DatabaseException, QueryPlanException {
    Database.Transaction transaction = this.database.beginTransaction();
    QueryPlan queryPlan = transaction.query(this.defaulTableName);

    queryPlan.select("int", QueryPlan.PredicateOperator.EQUALS, new IntDataBox(25));

    List<String> columnNames = new ArrayList<String>();
    columnNames.add("float");
    columnNames.add("int");
    queryPlan.project(columnNames);

    Iterator<Record> recordIterator = queryPlan.executeOptimal();

    while (recordIterator.hasNext()) {
      Record record = recordIterator.next();
      List<DataBox> values = record.getValues();

      assertEquals(2, values.size());
      assertTrue(values.get(0) instanceof FloatDataBox);
      assertTrue(values.get(1) instanceof IntDataBox);

      assertTrue(values.get(1).getInt() == 25);
    }

    QueryOperator finalOperator = queryPlan.getFinalOperator();
    String tree = "type: PROJECT\n" +
            "columns: [float, int]\n" +
            "\ttype: SELECT\n" +
            "\tcolumn: testAllTypes.int\n" +
            "\toperator: EQUALS\n" +
            "\tvalue: 25\n" +
            "\t\ttype: SEQSCAN\n" +
            "\t\ttable: testAllTypes";
    assertEquals(tree, finalOperator.toString());

    transaction.end();
  }

  @Test
  @Category(StudentTestP4.class)
  public void testMultipleSelects() throws DatabaseException, QueryPlanException {
    Database.Transaction transaction = this.database.beginTransaction();
    QueryPlan queryPlan = transaction.query(this.defaulTableName);

    queryPlan.select("int", QueryPlan.PredicateOperator.GREATER_THAN_EQUALS, new IntDataBox(25));
    queryPlan.select("int", QueryPlan.PredicateOperator.LESS_THAN_EQUALS, new IntDataBox(50));

    List<String> columnNames = new ArrayList<String>();
    columnNames.add("int");
    columnNames.add("bool");

    queryPlan.project(columnNames);

    Iterator<Record> outputIterator = queryPlan.executeOptimal();

    int count = 0;
    while (outputIterator.hasNext()) {
      Record record = outputIterator.next();
      assertTrue(record.getValues().get(0) instanceof IntDataBox);
      assertTrue(record.getValues().get(1) instanceof BoolDataBox);

      count++;
    }

    assertEquals(26, count);

    QueryOperator finalOperator = queryPlan.getFinalOperator();
    String tree = "type: PROJECT\n" +
            "columns: [int, bool]\n" +
            "\ttype: SELECT\n" +
            "\tcolumn: testAllTypes.int\n" +
            "\toperator: LESS_THAN_EQUALS\n" +
            "\tvalue: 50\n" +
            "\t\ttype: SELECT\n" +
            "\t\tcolumn: testAllTypes.int\n" +
            "\t\toperator: GREATER_THAN_EQUALS\n" +
            "\t\tvalue: 25\n" +
            "\t\t\ttype: SEQSCAN\n" +
            "\t\t\ttable: testAllTypes";

    assertEquals(tree, finalOperator.toString());

    transaction.end();

  }

  @Test
  @Category(StudentTestP4.class)
  public void testMultipleSelectsComplex() throws DatabaseException, QueryPlanException {
    Database.Transaction transaction = this.database.beginTransaction();
    QueryPlan queryPlan = transaction.query(this.defaulTableName);

    queryPlan.select("int", QueryPlan.PredicateOperator.GREATER_THAN_EQUALS, new IntDataBox(5));
    queryPlan.select("int", QueryPlan.PredicateOperator.LESS_THAN_EQUALS, new IntDataBox(75));
    queryPlan.select("int", QueryPlan.PredicateOperator.GREATER_THAN, new IntDataBox(15));

    List<String> columnNames = new ArrayList<String>();
    columnNames.add("int");
    columnNames.add("string");

    queryPlan.project(columnNames);

    Iterator<Record> outputIterator = queryPlan.executeOptimal();

    int count = 0;
    while (outputIterator.hasNext()) {
      Record record = outputIterator.next();
      assertTrue(record.getValues().get(0) instanceof IntDataBox);
      assertTrue(record.getValues().get(1) instanceof StringDataBox);

      count++;
    }

    assertEquals(60, count);

    QueryOperator finalOperator = queryPlan.getFinalOperator();
    String tree = "type: PROJECT\n" +
            "columns: [int, string]\n" +
            "\ttype: SELECT\n" +
            "\tcolumn: testAllTypes.int\n" +
            "\toperator: GREATER_THAN\n" +
            "\tvalue: 15\n" +
            "\t\ttype: SELECT\n" +
            "\t\tcolumn: testAllTypes.int\n" +
            "\t\toperator: LESS_THAN_EQUALS\n" +
            "\t\tvalue: 75\n" +
            "\t\t\ttype: SELECT\n" +
            "\t\t\tcolumn: testAllTypes.int\n" +
            "\t\t\toperator: GREATER_THAN_EQUALS\n" +
            "\t\t\tvalue: 5\n" +
            "\t\t\t\ttype: SEQSCAN\n" +
            "\t\t\t\ttable: testAllTypes";

    assertEquals(tree, finalOperator.toString());

    transaction.end();

  }


  @Test(timeout=1000)
  public void testSimpleProjectIterator() throws DatabaseException, QueryPlanException {
    Database.Transaction transaction = this.database.beginTransaction();
    QueryPlan queryPlan = transaction.query(this.defaulTableName);

    List<String> columnNames = new ArrayList<String>();
    columnNames.add("int");
    columnNames.add("string");

    queryPlan.project(columnNames);
    Iterator<Record> outputIterator = queryPlan.executeOptimal();

    int count = 0;
    while (outputIterator.hasNext()) {
      Record record = outputIterator.next();
      assertTrue(record.getValues().get(0) instanceof IntDataBox);
      assertTrue(record.getValues().get(1) instanceof StringDataBox);

      count++;
    }

    assertEquals(this.defaultNumRecords, count);

    QueryOperator finalOperator = queryPlan.getFinalOperator();
    String tree = "type: PROJECT\n" +
                  "columns: [int, string]\n" +
                  "\ttype: SEQSCAN\n" +
                  "\ttable: testAllTypes";
    assertEquals(tree, finalOperator.toString());

    transaction.end();
  }

  @Test(timeout=1000)
  public void testSimpleSelectIterator() throws DatabaseException, QueryPlanException {
    Database.Transaction transaction = this.database.beginTransaction();
    QueryPlan queryPlan = transaction.query(this.defaulTableName);

    queryPlan.select("int", QueryPlan.PredicateOperator.GREATER_THAN_EQUALS, new IntDataBox(0));

    Iterator<Record> outputIterator = queryPlan.executeOptimal();

    while (outputIterator.hasNext()) {
      Record record = outputIterator.next();

      assertTrue(record.getValues().get(1).getInt() >= 0);
    }

    QueryOperator finalOperator = queryPlan.getFinalOperator();
    String tree = "type: SELECT\n" +
                  "column: testAllTypes.int\n" +
                  "operator: GREATER_THAN_EQUALS\n" +
                  "value: 0\n" +
                  "\ttype: SEQSCAN\n" +
                  "\ttable: testAllTypes";
    assertEquals(tree, finalOperator.toString());

    transaction.end();
  }

  @Test(timeout=60000)
  public void testSimpleGroupByIterator() throws DatabaseException, QueryPlanException {
    Database.Transaction transaction = this.database.beginTransaction();
    QueryPlan queryPlan = transaction.query(this.defaulTableName);
    MarkerRecord markerRecord = MarkerRecord.getMarker();

    queryPlan.groupBy("int");
    Iterator<Record> outputIterator = queryPlan.executeOptimal();

    boolean first = true;
    int prevValue = 0;
    while (outputIterator.hasNext()) {
      Record record = outputIterator.next();

      if (first) {
        prevValue = record.getValues().get(1).getInt();
        first = false;
      } else if (record == markerRecord) {
        first = true;
      } else {
        assertEquals(prevValue, record.getValues().get(1).getInt());
      }
    }

    QueryOperator finalOperator = queryPlan.getFinalOperator();
    String tree = "type: GROUPBY\n" +
                  "column: testAllTypes.int\n" +
                  "\ttype: SEQSCAN\n" +
                  "\ttable: testAllTypes";
    assertEquals(tree, finalOperator.toString());

    transaction.end();
  }

  @Test(timeout=1000)
  public void testProjectSelectIterator() throws DatabaseException, QueryPlanException {
    Database.Transaction transaction = this.database.beginTransaction();
    QueryPlan queryPlan = transaction.query(this.defaulTableName);

    queryPlan.select("int", QueryPlan.PredicateOperator.GREATER_THAN_EQUALS, new IntDataBox(0));

    List<String> columnNames = new ArrayList<String>();
    columnNames.add("bool");
    columnNames.add("int");
    queryPlan.project(columnNames);

    Iterator<Record> recordIterator = queryPlan.executeOptimal();

    while (recordIterator.hasNext()) {
      Record record = recordIterator.next();
      List<DataBox> values = record.getValues();

      assertEquals(2, values.size());
      assertTrue(values.get(0) instanceof BoolDataBox);
      assertTrue(values.get(1) instanceof IntDataBox);

      assertTrue(values.get(1).getInt() >= 0);
    }

    QueryOperator finalOperator = queryPlan.getFinalOperator();
    String tree = "type: PROJECT\n" +
                  "columns: [bool, int]\n" +
                  "\ttype: SELECT\n" +
                  "\tcolumn: testAllTypes.int\n" +
                  "\toperator: GREATER_THAN_EQUALS\n" +
                  "\tvalue: 0\n" +
                  "\t\ttype: SEQSCAN\n" +
                  "\t\ttable: testAllTypes";
    assertEquals(tree, finalOperator.toString());

    transaction.end();
  }

}
