// Copyright (C) 2014 Miquel Sabaté Solà <mikisabate@gmail.com>
// This file is licensed under the MIT license.
// See the LICENSE file.

package com.mssola.storm.basic


// Storm imports.
import backtype.storm.task.TopologyContext;
import backtype.storm.{Config, LocalCluster}
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.tuple.{Fields, Tuple, Values}
import backtype.storm.topology.base.{BaseBasicBolt, BaseRichSpout}
import backtype.storm.topology.{TopologyBuilder, BasicOutputCollector, OutputFieldsDeclarer}

// Standard things from Java and Scala.
import util.Random
import java.util.Map
import collection.mutable.{HashMap}


class RandomSentenceSpout extends BaseRichSpout {
    var _collector: SpoutOutputCollector = _
    val _sentences = List("the cow jumped over the moon",
                          "an apple a day keeps the doctor away",
                          "four score and seven years ago",
                          "snow white and the seven dwarfs",
                          "i am at two with nature")

    def open(conf: Map[_,_], context: TopologyContext, collector: SpoutOutputCollector) = {
        _collector = collector
    }

    def nextTuple() = {
        Thread.sleep(100);
        val sentence = _sentences(Random.nextInt(_sentences.length))
        _collector.emit(new Values(sentence));
    }

    def declareOutputFields(declarer: OutputFieldsDeclarer) = {
        declarer.declare(new Fields("word"))
    }
}

class SplitSentence extends BaseBasicBolt {
    def execute(t: Tuple, collector: BasicOutputCollector) = {
        t.getString(0).split(" ").foreach {
            word => collector.emit(new Values(word))
        }
    }

    def declareOutputFields(declarer: OutputFieldsDeclarer) = {
        declarer.declare(new Fields("word"))
    }
}

class WordCount extends BaseBasicBolt {
    var counts = new HashMap[String, Integer]().withDefaultValue(0)

    def execute(t: Tuple, collector: BasicOutputCollector) {
        val word = t.getString(0)
        counts(word) += 1
        collector.emit(new Values(word, counts(word)))
    }

    def declareOutputFields(declarer: OutputFieldsDeclarer) = {
        declarer.declare(new Fields("word", "count"));
    }
}

object BasicTopology {
    def main(args: Array[String]) = {
        val builder = new TopologyBuilder
        builder.setSpout("randsentence", new RandomSentenceSpout, 5)
        builder.setBolt("split", new SplitSentence, 8)
        .shuffleGrouping("randsentence")
        builder.setBolt("count", new WordCount, 12)
        .fieldsGrouping("split", new Fields("word"))

        val conf = new Config
        conf.setDebug(true)
        conf.setMaxTaskParallelism(3)

        val cluster = new LocalCluster
        cluster.submitTopology("word-count", conf, builder.createTopology)
        Thread.sleep(10000)
        cluster.shutdown
    }
}
