package com.atguigu.spark.core.project.app

import com.atguigu.spark.core.project.bean.{CategroyCount, SessionInfo, UserVisitAction}
import org.apache.spark.rdd.RDD
import org.apache.spark.{Partitioner, SparkContext}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Author atguigu
 * Date 2020/5/11 14:59
 */
object CategorySessionTopApp {
    def statCategoryTop10Session(sc: SparkContext,
                                 categoryCountList: List[CategroyCount],
                                 userVisitActionRDD: RDD[UserVisitAction]) = {
        // 1. 过滤出来 top10品类的所有点击记录
        // 1.1 先map出来top10的品类id
        val cids = categoryCountList.map(_.cid.toLong)
        val topCategoryActionRDD: RDD[UserVisitAction] = userVisitActionRDD.filter(action => cids.contains(action.click_category_id))
        // 2. 计算每个品类 下的每个session 的点击量  rdd ((cid, sid) ,1)
        val cidAndSidCount = topCategoryActionRDD
            .map(action => ((action.click_category_id, action.session_id), 1))
            .reduceByKey(_ + _)
            .map {
                case ((cid, sid), count) => (cid, (sid, count))
            }
        //   3. 按照品类分组,
        val cidAndSidCountGrouped: RDD[(Long, Iterable[(String, Int)])] = cidAndSidCount.groupByKey()
        // 4. 排序, 取top10
        val result = cidAndSidCountGrouped.map {
            case (cid, sidCountIt) =>
                // sidCountIt 排序, 取前10
                // Iterable转成容器式集合的时候, 如果数据量过大, 极有可能导致oom
                (cid, sidCountIt.toList.sortBy(-_._2).take(5))
        }
        
        result.collect.foreach(println)
    }
    
    def statCategoryTop10Session_1(sc: SparkContext,
                                   categoryCountList: List[CategroyCount],
                                   userVisitActionRDD: RDD[UserVisitAction]) = {
        
        val cids = categoryCountList.map(_.cid.toLong)
        val topCategoryActionRDD: RDD[UserVisitAction] = userVisitActionRDD.filter(action => cids.contains(action.click_category_id))
        
        val cidAndSidCount: RDD[(Long, (String, Int))] = topCategoryActionRDD
            .map(action => ((action.click_category_id, action.session_id), 1))
            .reduceByKey(_ + _)
            .map {
                case ((cid, sid), count) => (cid, (sid, count))
            }
        
        // cid1 cid2
        // 5. 分别过滤出来没给品类的数据, 然后使用rdd的排序功能
        cidAndSidCount.cache()
        val buffer = ListBuffer[(Long, List[(String, Int)])]()
        for (cid <- cids) {
            /*
            List((15,(632972a4-f811-4000-b920-dc12ea803a41,10)), (15,(f34878b8-1784-4d81-a4d1-0c93ce53e942,8)), (15,(5e3545a0-1521-4ad6-91fe-e792c20c46da,8)), (15,(66a421b0-839d-49ae-a386-5fa3ed75226f,8)), (15,(9fa653ec-5a22-4938-83c5-21521d083cd0,8)))
            目标:
            (9,List((199f8e1d-db1a-4174-b0c2-ef095aaef3ee,9), (329b966c-d61b-46ad-949a-7e37142d384a,8), (5e3545a0-1521-4ad6-91fe-e792c20c46da,8), (e306c00b-a6c5-44c2-9c77-15e919340324,7), (bed60a57-3f81-4616-9e8b-067445695a77,7)))
             */
            val arr = cidAndSidCount.filter(cid == _._1)
                .sortBy(-_._2._2)
                .take(5)
                .map(_._2)
            buffer += ((cid, arr.toList))
        }
        buffer.foreach(println)
        
    }
    
    def statCategoryTop10Session_2(sc: SparkContext,
                                   categoryCountList: List[CategroyCount],
                                   userVisitActionRDD: RDD[UserVisitAction]) = {
        // 1. 过滤出来 top10品类的所有点击记录
        // 1.1 先map出来top10的品类id
        val cids = categoryCountList.map(_.cid.toLong)
        val topCategoryActionRDD: RDD[UserVisitAction] = userVisitActionRDD.filter(action => cids.contains(action.click_category_id))
        // 2. 计算每个品类 下的每个session 的点击量  rdd ((cid, sid) ,1)
        val cidAndSidCount: RDD[(Long, (String, Int))] = topCategoryActionRDD
            .map(action => ((action.click_category_id, action.session_id), 1))
            .reduceByKey(_ + _)
            .map {
                case ((cid, sid), count) => (cid, (sid, count))
            }
        //   3. 按照品类分组,
        val cidAndSidCountGrouped: RDD[(Long, Iterable[(String, Int)])] = cidAndSidCount.groupByKey()
        // 4. 排序, 取top10
        val result = cidAndSidCountGrouped.map {
            case (cid, sidCountIt) =>
                // sidCountIt 要排序, 但是又不想转成容器式的集合? 怎么做?
                // 如果不转, 绝对不能用scala的sortBy !
                // 找一个可以自动排序的集合(TreeSet), 只需要让TreeSet集合的长度保持10就行了.
                var set: mutable.TreeSet[SessionInfo] = mutable.TreeSet[SessionInfo]()
                sidCountIt.foreach {
                    case (sid, count) =>
                        val info: SessionInfo = SessionInfo(sid, count)
                        set += info
                        if (set.size > 10) set = set.take(10)
                }
                (cid, set.toList)
        }
        
        result.collect.foreach(println)
        
        Thread.sleep(1000000)
    }
    
    def statCategoryTop10Session_3(sc: SparkContext,
                                   categoryCountList: List[CategroyCount],
                                   userVisitActionRDD: RDD[UserVisitAction]) = {
        // 1. 过滤出来 top10品类的所有点击记录
        // 1.1 先map出来top10的品类id
        val cids = categoryCountList.map(_.cid.toLong)
        val topCategoryActionRDD: RDD[UserVisitAction] = userVisitActionRDD.filter(action => cids.contains(action.click_category_id))
        // 2. 计算每个品类 下的每个session 的点击量  rdd ((cid, sid) ,1)
        val cidAndSidCount: RDD[(Long, (String, Int))] = topCategoryActionRDD
            .map(action => ((action.click_category_id, action.session_id), 1))
            // 使用自定义分区器  重点理解分区器的原理
            .reduceByKey(new CategoryPartitioner(cids), _ + _)
            .map {
                case ((cid, sid), count) => (cid, (sid, count))
            }
        
        // 3. 排序取top10
        val result = cidAndSidCount.mapPartitions((it: Iterator[(Long, (String, Int))]) => {
            
            var treeSet: mutable.Set[SessionInfo] = mutable.TreeSet[SessionInfo]()
            var id = 0L
            it.foreach {
                case (cid, (sid, count)) =>
                    id = cid
                    treeSet += SessionInfo(sid, count)
                    if (treeSet.size > 10) treeSet = treeSet.take(10)
            }
//            treeSet.toIterator.map((id, _))
            Iterator((id, treeSet.toList))
        })
    
        result.collect.foreach(println)
        
        Thread.sleep(1000000)
    }
}

/*
有几个品类就应该有几个分区?
 */
class CategoryPartitioner(cids: List[Long]) extends Partitioner {
    // 用cid索引, 作为将来他的分区索引.
    private val cidWithIndex: Map[Long, Int] = cids.zipWithIndex.toMap
    
    // 返回集合的长度
    override def numPartitions: Int = cids.length
    
    // 根据key返回分区的索引
    override def getPartition(key: Any): Int = {
        key match {
            // 根据品类id返回分区的索引!    0-9
            case (cid: Long, _) =>
                //  cid % 10 toInt    //  错误: 10 20 30 会进入到同一个分区
                cidWithIndex(cid)
        }
    }
}

