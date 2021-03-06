/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.rdd

import org.apache.spark._

class DoubleRDDSuite extends SparkFunSuite with SharedSparkContext {
  test("sum") {//总和
    assert(sc.parallelize(Seq.empty[Double]).sum() === 0.0)
    assert(sc.parallelize(Seq(1.0)).sum() === 1.0)
    assert(sc.parallelize(Seq(1.0, 2.0)).sum() === 3.0) //求和
    val rdd = sc.parallelize(Seq(1.0, 2.0)).map(_.toDouble)//转换DoubleRDD
    val stats = rdd.stats()
    println("最大值:" + stats.max, "最小值:" + stats.min, "平均值:" + stats.mean, "方差" + stats.variance,
      "标准方差" + stats.stdev, "元素个数:" + stats.count)
  }

  // Verify tests on the histogram functionality. We test with both evenly
  // and non-evenly spaced buckets as the bucket lookup function changes.
  //关于直方图功能的验证测试,我们测试的均匀和非均匀分布的桶桶查找功能的变化
  test("WorksOnEmpty") {
    // Make sure that it works on an empty input
    val rdd: RDD[Double] = sc.parallelize(Seq())
    //
    //直方图展示数据生成,,下面俩种用法表示一致,共分为一个桶:( 0,10)
    val buckets = Array(0.0, 10.0)
    /**
     * histogram
     * 针对数据类型为Double的RDD统计直方图。可以输入分桶数进行平均分桶,也可以输入自定义的分桶区间。
     * 平均分桶情况下会输出两个数组,第一个是每个分桶边界值,第二个是每个分桶的统计数。
     * 自定义分桶情况下只输出一个数组,即每个分桶的统计数。
     */
    val histogramResults = rdd.histogram(buckets)
    val histogramResults2 = rdd.histogram(buckets, true)
    val expectedHistogramResults = Array(0)
    assert(histogramResults === expectedHistogramResults)
    assert(histogramResults2 === expectedHistogramResults)
    val emptyRDD: RDD[Double] = sc.emptyRDD
    assert(emptyRDD.histogram(buckets) === expectedHistogramResults)
    assert(emptyRDD.histogram(buckets, true) === expectedHistogramResults)
  }

  test("WorksWithOutOfRangeWithOneBucket") {
    // Verify that if all of the elements are out of range the counts are zero
    //确认如果所有的元素都是不在范围内的计数是
    val rdd = sc.parallelize(Seq(10.01, -0.01))
    val buckets = Array(0.0, 10.0) //只有一个范围
    //Array[Long] = Array(0)
    val histogramResults = rdd.histogram(buckets) //直方图    
    val histogramResults2 = rdd.histogram(buckets, true)
    val expectedHistogramResults = Array(0)
    assert(histogramResults === expectedHistogramResults)
    assert(histogramResults2 === expectedHistogramResults)
  }

  test("WorksInRangeWithOneBucket") {
    // Verify the basic case of one bucket and all elements in that bucket works
    //验证一个桶和该桶中的所有元素的基本情况
    val rdd = sc.parallelize(Seq(1, 2, 3, 4))
    val buckets = Array(0.0, 10.0) //每个分桶边界值
    val histogramResults = rdd.histogram(buckets) //直方图
    val histogramResults2 = rdd.histogram(buckets, true)
    val expectedHistogramResults = Array(4)
    assert(histogramResults === expectedHistogramResults)
    assert(histogramResults2 === expectedHistogramResults)
  }

  test("WorksInRangeWithOneBucketExactMatch") {
    // Verify the basic case of one bucket and all elements in that bucket works
    //验证一个桶和该桶中的所有元素的基本情况
    val rdd = sc.parallelize(Seq(1, 2, 3, 4))
    val buckets = Array(1.0, 4.0) //匹配
    val histogramResults = rdd.histogram(buckets) //直方图
    val histogramResults2 = rdd.histogram(buckets, true) //直方图
    val expectedHistogramResults = Array(4)
    assert(histogramResults === expectedHistogramResults)
    assert(histogramResults2 === expectedHistogramResults)
  }

  test("WorksWithOutOfRangeWithTwoBuckets") {
    // Verify that out of range works with two buckets
    //用两个桶检查出范围内的工作
    val rdd = sc.parallelize(Seq(10.01, -0.01))
    val buckets = Array(0.0, 5.0, 10.0) //第一个是每个分桶边界值,第二个是每个分桶的统计数
    val histogramResults = rdd.histogram(buckets)
    val histogramResults2 = rdd.histogram(buckets, true)
    val expectedHistogramResults = Array(0, 0)
    assert(histogramResults === expectedHistogramResults)
    assert(histogramResults2 === expectedHistogramResults)
  }

  test("WorksWithOutOfRangeWithTwoUnEvenBuckets") {
    // Verify that out of range works with two un even buckets
    val rdd = sc.parallelize(Seq(10.01, -0.01))
    val buckets = Array(0.0, 4.0, 10.0) //第一个是每个分桶边界值,第二个是每个分桶的统计数
    val histogramResults = rdd.histogram(buckets)
    val expectedHistogramResults = Array(0, 0)
    assert(histogramResults === expectedHistogramResults)
  }

  test("WorksInRangeWithTwoBuckets") {
    // Make sure that it works with two equally spaced buckets and elements in each
    val rdd = sc.parallelize(Seq(1, 2, 3, 5, 6))
    val buckets = Array(0.0, 5.0, 10.0) //第一个是每个分桶边界值,第二个是每个分桶的统计数
    val histogramResults = rdd.histogram(buckets)
    val histogramResults2 = rdd.histogram(buckets, true)
    val expectedHistogramResults = Array(3, 2)
    assert(histogramResults === expectedHistogramResults)
    assert(histogramResults2 === expectedHistogramResults)
  }

  test("WorksInRangeWithTwoBucketsAndNaN") {
    // Make sure that it works with two equally spaced buckets and elements in each
    val rdd = sc.parallelize(Seq(1, 2, 3, 5, 6, Double.NaN))
    val buckets = Array(0.0, 5.0, 10.0) //第一个是每个分桶边界值,第二个是每个分桶的统计数
    val histogramResults = rdd.histogram(buckets)
    val histogramResults2 = rdd.histogram(buckets, true)
    val expectedHistogramResults = Array(3, 2)
    assert(histogramResults === expectedHistogramResults)
    assert(histogramResults2 === expectedHistogramResults)
  }

  test("WorksInRangeWithTwoUnevenBuckets") {
    // Make sure that it works with two unequally spaced buckets and elements in each
    val rdd = sc.parallelize(Seq(1, 2, 3, 5, 6))
    val buckets = Array(0.0, 5.0, 11.0) //第一个是每个分桶边界值,第二个是每个分桶的统计数
    val histogramResults = rdd.histogram(buckets)
    val expectedHistogramResults = Array(3, 2)
    assert(histogramResults === expectedHistogramResults)
  }

  test("WorksMixedRangeWithTwoUnevenBuckets") {
    // Make sure that it works with two unequally spaced buckets and elements in each
    val rdd = sc.parallelize(Seq(-0.01, 0.0, 1, 2, 3, 5, 6, 11.0, 11.01))
    val buckets = Array(0.0, 5.0, 11.0) //第一个是每个分桶边界值,第二个是每个分桶的统计数
    val histogramResults = rdd.histogram(buckets)
    val expectedHistogramResults = Array(4, 3)
    assert(histogramResults === expectedHistogramResults)
  }

  test("WorksMixedRangeWithFourUnevenBuckets") {
    // Make sure that it works with two unequally spaced buckets and elements in each
    val rdd = sc.parallelize(Seq(-0.01, 0.0, 1, 2, 3, 5, 6, 11.01, 12.0, 199.0,
      200.0, 200.1))
    val buckets = Array(0.0, 5.0, 11.0, 12.0, 200.0)
    val histogramResults = rdd.histogram(buckets)
    val expectedHistogramResults = Array(4, 2, 1, 3)
    assert(histogramResults === expectedHistogramResults)
  }

  test("WorksMixedRangeWithUnevenBucketsAndNaN") {
    // Make sure that it works with two unequally spaced buckets and elements in each
    val rdd = sc.parallelize(Seq(-0.01, 0.0, 1, 2, 3, 5, 6, 11.01, 12.0, 199.0,
      200.0, 200.1, Double.NaN))
    val buckets = Array(0.0, 5.0, 11.0, 12.0, 200.0)
    val histogramResults = rdd.histogram(buckets)
    val expectedHistogramResults = Array(4, 2, 1, 3)
    assert(histogramResults === expectedHistogramResults)
  }
  // Make sure this works with a NaN end bucket
  test("WorksMixedRangeWithUnevenBucketsAndNaNAndNaNRange") {
    // Make sure that it works with two unequally spaced buckets and elements in each
    val rdd = sc.parallelize(Seq(-0.01, 0.0, 1, 2, 3, 5, 6, 11.01, 12.0, 199.0,
      200.0, 200.1, Double.NaN))
    val buckets = Array(0.0, 5.0, 11.0, 12.0, 200.0, Double.NaN)
    val histogramResults = rdd.histogram(buckets)
    val expectedHistogramResults = Array(4, 2, 1, 2, 3)
    assert(histogramResults === expectedHistogramResults)
  }
  // Make sure this works with a NaN end bucket and an inifity
  test("WorksMixedRangeWithUnevenBucketsAndNaNAndNaNRangeAndInfity") {
    // Make sure that it works with two unequally spaced buckets and elements in each
    val rdd = sc.parallelize(Seq(-0.01, 0.0, 1, 2, 3, 5, 6, 11.01, 12.0, 199.0,
      200.0, 200.1, 1.0 / 0.0, -1.0 / 0.0, Double.NaN))
    val buckets = Array(0.0, 5.0, 11.0, 12.0, 200.0, Double.NaN)
    val histogramResults = rdd.histogram(buckets)
    val expectedHistogramResults = Array(4, 2, 1, 2, 4)
    assert(histogramResults === expectedHistogramResults)
  }

  test("WorksWithOutOfRangeWithInfiniteBuckets") {
    // Verify that out of range works with two buckets
    val rdd = sc.parallelize(Seq(10.01, -0.01, Double.NaN))
    val buckets = Array(-1.0 / 0.0, 0.0, 1.0 / 0.0)
    val histogramResults = rdd.histogram(buckets)
    val expectedHistogramResults = Array(1, 1)
    assert(histogramResults === expectedHistogramResults)
  }
  // Test the failure mode with an invalid bucket array
  test("ThrowsExceptionOnInvalidBucketArray") {
    val rdd = sc.parallelize(Seq(1.0))
    // Empty array
    intercept[IllegalArgumentException] {
      val buckets = Array.empty[Double]
      val result = rdd.histogram(buckets)
    }
    // Single element array
    intercept[IllegalArgumentException] {
      val buckets = Array(1.0)
      val result = rdd.histogram(buckets)
    }
  }

  // Test automatic histogram function
  test("WorksWithoutBucketsBasic") {
    // Verify the basic case of one bucket and all elements in that bucket works
    val rdd = sc.parallelize(Seq(1, 2, 3, 4))
    val (histogramBuckets, histogramResults) = rdd.histogram(1)
    val expectedHistogramResults = Array(4)
    val expectedHistogramBuckets = Array(1.0, 4.0)
    assert(histogramResults === expectedHistogramResults)
    assert(histogramBuckets === expectedHistogramBuckets)
  }
  // Test automatic histogram function with a single element
  test("WorksWithoutBucketsBasicSingleElement") {
    // Verify the basic case of one bucket and all elements in that bucket works
    val rdd = sc.parallelize(Seq(1))
    val (histogramBuckets, histogramResults) = rdd.histogram(1)
    val expectedHistogramResults = Array(1)
    val expectedHistogramBuckets = Array(1.0, 1.0)
    assert(histogramResults === expectedHistogramResults)
    assert(histogramBuckets === expectedHistogramBuckets)
  }
  // Test automatic histogram function with a single element
  test("WorksWithoutBucketsBasicNoRange") {
    // Verify the basic case of one bucket and all elements in that bucket works
    val rdd = sc.parallelize(Seq(1, 1, 1, 1))
    val (histogramBuckets, histogramResults) = rdd.histogram(1)
    val expectedHistogramResults = Array(4)
    val expectedHistogramBuckets = Array(1.0, 1.0)
    assert(histogramResults === expectedHistogramResults)
    assert(histogramBuckets === expectedHistogramBuckets)
  }

  test("WorksWithoutBucketsBasicTwo") {
    // Verify the basic case of one bucket and all elements in that bucket works
    val rdd = sc.parallelize(Seq(1, 2, 3, 4))
    val (histogramBuckets, histogramResults) = rdd.histogram(2)
    val expectedHistogramResults = Array(2, 2)
    val expectedHistogramBuckets = Array(1.0, 2.5, 4.0)
    assert(histogramResults === expectedHistogramResults)
    assert(histogramBuckets === expectedHistogramBuckets)
  }

  test("WorksWithDoubleValuesAtMinMax") {
    val rdd = sc.parallelize(Seq(1, 1, 1, 2, 3, 3))
    assert(Array(3, 0, 1, 2) === rdd.map(_.toDouble).histogram(4)._2)
    assert(Array(3, 1, 2) === rdd.map(_.toDouble).histogram(3)._2)
  }

  test("WorksWithoutBucketsWithMoreRequestedThanElements") {
    // Verify the basic case of one bucket and all elements in that bucket works
    val rdd = sc.parallelize(Seq(1, 2))
    val (histogramBuckets, histogramResults) = rdd.histogram(10)
    val expectedHistogramResults =
      Array(1, 0, 0, 0, 0, 0, 0, 0, 0, 1)
    val expectedHistogramBuckets =
      Array(1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0)
    assert(histogramResults === expectedHistogramResults)
    assert(histogramBuckets === expectedHistogramBuckets)
  }

  test("WorksWithoutBucketsForLargerDatasets") {
    // Verify the case of slightly larger datasets
    val rdd = sc.parallelize(6 to 99)
    val (histogramBuckets, histogramResults) = rdd.histogram(8)
    val expectedHistogramResults =
      Array(12, 12, 11, 12, 12, 11, 12, 12)
    val expectedHistogramBuckets =
      Array(6.0, 17.625, 29.25, 40.875, 52.5, 64.125, 75.75, 87.375, 99.0)
    assert(histogramResults === expectedHistogramResults)
    assert(histogramBuckets === expectedHistogramBuckets)
  }

  test("WorksWithoutBucketsWithNonIntegralBucketEdges") {
    // Verify the case of buckets with nonintegral edges. See #SPARK-2862.
    val rdd = sc.parallelize(6 to 99)
    val (histogramBuckets, histogramResults) = rdd.histogram(9)
    // Buckets are 6.0, 16.333333333333336, 26.666666666666668, 37.0, 47.333333333333336 ...
    val expectedHistogramResults =
      Array(11, 10, 10, 11, 10, 10, 11, 10, 11)
    assert(histogramResults === expectedHistogramResults)
    assert(histogramBuckets(0) === 6.0)
    assert(histogramBuckets(9) === 99.0)
  }

  test("WorksWithHugeRange") {
    val rdd = sc.parallelize(Array(0, 1.0e24, 1.0e30))
    val histogramResults = rdd.histogram(1000000)._2
    assert(histogramResults(0) === 1)
    assert(histogramResults(1) === 1)
    assert(histogramResults.last === 1)
    assert((2 to histogramResults.length - 2).forall(i => histogramResults(i) == 0))
  }

  // Test the failure mode with an invalid RDD
  test("ThrowsExceptionOnInvalidRDDs") {
    // infinity
    intercept[UnsupportedOperationException] {
      val rdd = sc.parallelize(Seq(1, 1.0 / 0.0))
      val result = rdd.histogram(1)
    }
    // NaN
    intercept[UnsupportedOperationException] {
      val rdd = sc.parallelize(Seq(1, Double.NaN))
      val result = rdd.histogram(1)
    }
    // Empty
    intercept[UnsupportedOperationException] {
      val rdd: RDD[Double] = sc.parallelize(Seq())
      val result = rdd.histogram(1)
    }
  }

}
