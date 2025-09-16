package org.husonlab.diamer.main.encoders;

import org.husonlab.diamer.indexing.kmers.KmerExtractor;
import org.husonlab.diamer.main.GlobalSettings;

public class EncoderWithoutKmerExtractor extends Encoder {

    public EncoderWithoutKmerExtractor(GlobalSettings globalSettings) {
        super(globalSettings);
    }

    @Override
    public KmerExtractor getKmerExtractor() {
        throw new UnsupportedOperationException("Kmer extraction is not supported in this encoder.");
    }
}
