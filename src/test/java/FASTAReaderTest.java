//import org.husonlab.diamer2.io.seq.FASTAReader;
//import org.husonlab.diamer2.io.seq.SequenceSupplier;
//import org.husonlab.diamer2.seq.SequenceRecord;
//import org.junit.Test;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//
//import static org.junit.Assert.assertEquals;
//
//public class FASTAReaderTest {
//    @Test
//    public void testFASTAReader() throws IOException {
//        try (SequenceSupplier sup = new SequenceSupplier(new FASTAReader(new File("src/test/resources/database/db.fsa")), false) ) {
//            assertEquals(new SequenceRecord(
//                    ">A0A075B700.2 >A0A096XJN4.1 schould be merged to taxon1 (LCA of 4 and 5)",
//                    "NQFLFAGIELILRKYEITVYQLSADDLRSHKVRKDHVFFIECPLREENLHSLRKLYQYGARVCYLMRSSIECDRKNASQF" +
//                            "IDITTEMNVFIAKVLKTINNSACPPAVNIVRLTNQEFSVGRLVLCGHSDVDIASELLITIRSSQDHINRVLKKLGGKSVAD" +
//                            "IYLQRNVIYGSGTTLQKQKSR"
//            ), sup.next());
//
//            assertEquals(new SequenceRecord(
//                    ">A0A023PXE5.1",
//                    """
//                            MPPSRPAKRSADGGGISDDDTAALGGGKSKQARSDRGPEDFSSVVKNRLQSYSRTGQACDRCKVRKIRCDALAEGCSHCI
//                            NLNLECYVTDRVTGRTERRGYLQQLEREKNSMLTHIRDLERLCYEITVYQLSADDLRSSAQSPSEPGAGELPGSTGGGSK
//                            LTDGWSRYGALWIKYASTSQPADATIRPRIPQREWQSRPDQICWGVVGDDAPFSSLKGTTLTLLGTTIETTSFDAPDIDE
//                            PAAGVDSSMPLYNKSMLAFLRSSMGVNPVVQAEL*PSRENAFMYAEWY123/()FISVACFLPLLHKPTFFKLVSSSCCF"""
//            ), sup.next());
//
//            assertEquals(new SequenceRecord(
//                    ">A0A023PXH4.1",
//                    """
//                            MITNFFIPELNNHDVQELWFQQDGATCHTARAIIDLLKDTFGDRLISRFGPVKWPPRSCDLTPLDYFLWGYVKSLVSADK
//                            PQMLDHLEDNIRRVIADIRPQMLENVI"""
//            ), sup.next());
//
//            ArrayList<SequenceRecord> sequenceRecords = sup.next(Integer.MAX_VALUE);
//            assertEquals(4, sequenceRecords.size());
//        }
//    }
//}
