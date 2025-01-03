import org.husonlab.diamer2.io.FASTAReader;
import org.husonlab.diamer2.io.SequenceSupplier;
import org.husonlab.diamer2.seq.Sequence;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class FASTAReaderTest {
    @Test
    public void testFASTAReader() throws IOException {
        try (SequenceSupplier sup = new SequenceSupplier(new FASTAReader(new File("src/test/resources/database/db.fsa")), false) ) {
            assertEquals(new Sequence(
                    ">A0A075B700.2 >A0A096XJN4.1 schould be merged to taxon1 (LCA of 4 and 5)",
                    "NQFLFAGIELILRKYEITVYQLSADDLRSHKVRKDHVFFIECPLREENLHSLRKLYQYGARVCYLMRSSIECDRKNASQF" +
                            "IDITTEMNVFIAKVLKTINNSACPPAVNIVRLTNQEFSVGRLVLCGHSDVDIASELLITIRSSQDHINRVLKKLGGKSVAD" +
                            "IYLQRNVIYGSGTTLQKQKSR"
            ), sup.next());

            assertEquals(new Sequence(
                    ">A0A023PXE5.1",
                    """
                            MPPSRPAKRSADGGGISDDDTAALGGGKSKQARSDRGPEDFSSVVKNRLQSYSRTGQACDRCKVRKIRCDALAEGCSHCI
                            NLNLECYVTDRVTGRTERRGYLQQLEREKNSMLTHIRDLERLCYEITVYQLSADDLRSSAQSPSEPGAGELPGSTGGGSK
                            LTDGWSRYGALWIKYASTSQPADATIRPRIPQREWQSRPDQICWGVVGDDAPFSSLKGTTLTLLGTTIETTSFDAPDIDE
                            PAAGVDSSMPLYNKSMLAFLRSSMGVNPVVQAEL*PSRENAFMYAEWY123/()FISVACFLPLLHKPTFFKLVSSSCCF"""
            ), sup.next());

            assertEquals(new Sequence(
                    ">A0A023PXH4.1",
                    """
                            MITNFFIPELNNHDVQELWFQQDGATCHTARAIIDLLKDTFGDRLISRFGPVKWPPRSCDLTPLDYFLWGYVKSLVSADK
                            PQMLDHLEDNIRRVIADIRPQMLENVI"""
            ), sup.next());

            ArrayList<Sequence> sequences = sup.next(Integer.MAX_VALUE);
            assertEquals(4, sequences.size());
        }
    }
}
