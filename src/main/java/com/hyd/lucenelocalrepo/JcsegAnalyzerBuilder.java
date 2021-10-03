package com.hyd.lucenelocalrepo;

import org.lionsoul.jcseg.ISegment;
import org.lionsoul.jcseg.analyzer.JcsegAnalyzer;
import org.lionsoul.jcseg.dic.HashMapDictionary;
import org.lionsoul.jcseg.segmenter.SegmenterConfig;

/**
 * @author yiding_he
 */
public class JcsegAnalyzerBuilder {

  public static JcsegAnalyzer build() {
    SegmenterConfig segmenterConfig = new SegmenterConfig();
    HashMapDictionary dictionary = new HashMapDictionary(segmenterConfig, true);
    return new JcsegAnalyzer(ISegment.Type.COMPLEX, segmenterConfig, dictionary);
  }

}
