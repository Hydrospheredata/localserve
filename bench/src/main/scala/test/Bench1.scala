package test

import localserve._
import org.apache.spark.SparkConf
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.feature.{StringIndexer, VectorIndexer}
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.sql.{Row, SparkSession}
import org.openjdk.jmh.annotations.Benchmark

class Bench1 {

  import Bench1._

  @Benchmark
  def local(): Unit = {
    transformer(input)
  }

}

object Bench1 {

  val conf = new SparkConf()
    .setMaster("local[2]")
    .setAppName("test")
    .set("spark.ui.enabled", "false")

  val session: SparkSession = SparkSession.builder().config(conf).getOrCreate()

  val steps = Seq(
    new StringIndexer().setInputCol("label").setOutputCol("indexedLabel"),
    new VectorIndexer().setInputCol("features").setOutputCol("indexedFeatures").setMaxCategories(4),
    new RandomForestClassifier().setLabelCol("indexedLabel").setFeaturesCol("indexedFeatures").setNumTrees(10)
  )
  val data = session.createDataFrame(Seq(
    (Vectors.dense(4.0, 0.2, 3.0, 4.0, 5.0), 1.0),
    (Vectors.dense(3.0, 0.3, 1.0, 4.1, 5.0), 1.0),
    (Vectors.dense(2.0, 0.5, 3.2, 4.0, 5.0), 1.0),
    (Vectors.dense(5.0, 0.7, 1.5, 4.0, 5.0), 1.0),
    (Vectors.dense(1.0, 0.1, 7.0, 4.0, 5.0), 0.0),
    (Vectors.dense(8.0, 0.3, 5.0, 1.0, 7.0), 0.0)
  )).toDF("features", "label")

  val inputData = data.drop(data.col("label"))

  val pipeline = new Pipeline().setStages(steps.toArray)
  val pipelineModel = pipeline.fit(data)


  val emptyDf = session.createDataFrame(session.sparkContext.emptyRDD[Row], inputData.schema)
  val transformer = FastInterpreter.fromTransformer(pipelineModel, emptyDf)

  val input = PlainDataset(
    Column("features", Seq(
      Vectors.dense(4.0, 0.2, 3.0, 4.0, 5.0),
      Vectors.dense(3.0, 0.3, 1.0, 4.1, 5.0),
      Vectors.dense(2.0, 0.5, 3.2, 4.0, 5.0),
      Vectors.dense(5.0, 0.7, 1.5, 4.0, 5.0),
      Vectors.dense(1.0, 0.1, 7.0, 4.0, 5.0),
      Vectors.dense(8.0, 0.3, 5.0, 1.0, 7.0)
    ))
  )
}
