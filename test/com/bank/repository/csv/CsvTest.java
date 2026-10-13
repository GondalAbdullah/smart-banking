package com.bank.repository.csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvTest {

    @Test
    void plainFieldsAreJoinedWithCommas() {
        assertEquals("C0001,Ali Khan,Lahore", Csv.join(List.of("C0001", "Ali Khan", "Lahore")));
    }

    @Test
    void aFieldWithACommaOrQuoteIsQuoted() {
        assertEquals("\"House 5, Street 2\",\"The \"\"Mall\"\"\"",
                Csv.join(List.of("House 5, Street 2", "The \"Mall\"")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Lahore", "House 5, Street 2", "The \"Mall\"", "\"", ",", "", "a,\"b\",c"})
    void anyFieldSurvivesJoinAndSplit(String tricky) {
        List<String> fields = List.of("before", tricky, "after");

        assertEquals(fields, Csv.split(Csv.join(fields)));
    }

    @Test
    void emptyFieldsAreKept() {
        assertEquals(List.of("A00001", "", ""), Csv.split("A00001,,"));
    }

    @Test
    void aLineBreakInsideAFieldIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Csv.join(List.of("line 1\nline 2")));
    }

    @Test
    void brokenQuotesAreRefused() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> Csv.split("\"never closed,x")),
                () -> assertThrows(IllegalArgumentException.class, () -> Csv.split("\"closed\"too early,x")));
    }

    @Test
    void numbersAreWrittenWithoutScientificNotation() {
        assertEquals("1000", Csv.text(new BigDecimal("1E+3")));
        assertEquals("", Csv.text(null));
    }
}
