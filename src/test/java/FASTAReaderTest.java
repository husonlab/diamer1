import org.husonlab.diamer2.io.FASTAReader;
import org.husonlab.diamer2.seq.Sequence;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class FASTAReaderTest {
    @Test
    public void testFASTAReader() throws IOException {
        try (BufferedReader br = Files.newBufferedReader(Path.of("src/test/resources/testNCBI/db.fsa"));
             FASTAReader fastaReader = new FASTAReader(br)) {

            assertEquals(new Sequence(
                    ">A0A075B700.2 >A0A096XJN4.1 schould be merged to taxon1 (LCA of 4 and 5)",
                    "NQFLFAGIELILRKYEITVYQLSADDLRSHKVRKDHVFFIECPLREENLHSLRKLYQYGARVCYLMRSSIECDRKNASQF" +
                            "IDITTEMNVFIAKVLKTINNSACPPAVNIVRLTNQEFSVGRLVLCGHSDVDIASELLITIRSSQDHINRVLKKLGGKSVAD" +
                            "IYLQRNVIYGSGTTLQKQKSR"
            ), fastaReader.getNextSequence());

            assertEquals(new Sequence(
                    ">A0A023PXE5.1",
                    """
                            MPPSRPAKRSADGGGISDDDTAALGGGKSKQARSDRGPEDFSSVVKNRLQSYSRTGQACDRCKVRKIRCDALAEGCSHCI
                            NLNLECYVTDRVTGRTERRGYLQQLEREKNSMLTHIRDLERLCYEITVYQLSADDLRSSAQSPSEPGAGELPGSTGGGSK
                            LTDGWSRYGALWIKYASTSQPADATIRPRIPQREWQSRPDQICWGVVGDDAPFSSLKGTTLTLLGTTIETTSFDAPDIDE
                            PAAGVDSSMPLYNKSMLAFLRSSMGVNPVVQAEL*PSRENAFMYAEWY123/()FISVACFLPLLHKPTFFKLVSSSCCF"""
            ), fastaReader.getNextSequence());

            assertEquals(new Sequence(
                    ">A0A023PXH4.1",
                    """
                            MITNFFIPELNNHDVQELWFQQDGATCHTARAIIDLLKDTFGDRLISRFGPVKWPPRSCDLTPLDYFLWGYVKSLVSADK
                            PQMLDHLEDNIRRVIADIRPQMLENVI"""
            ), fastaReader.getNextSequence());

            ArrayList<Sequence> sequences = fastaReader.getNSequences(Integer.MAX_VALUE);
            assertEquals(4, sequences.size());
        }
    }
}
