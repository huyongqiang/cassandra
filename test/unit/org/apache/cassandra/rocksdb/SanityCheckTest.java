package org.apache.cassandra.rocksdb;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.SinglePartitionReadCommand;
import org.apache.cassandra.rocksdb.tools.RowIteratorSanityCheck;
import org.apache.cassandra.rocksdb.tools.SanityCheckUtils;

import static org.junit.Assert.assertEquals;

public class SanityCheckTest extends RocksDBTestBase
{
    @BeforeClass
    public static void classSetUp() throws Exception
    {
        RocksDBTestBase.classSetUp();
        System.setProperty("cassandra.rocksdb.double_write", "true");
    }

    @AfterClass
    public static void classTeardown() throws Exception
    {
        System.clearProperty("cassandra.rocksdb.double_write");
        RocksDBTestBase.classTeardown();
    }

    @Test
    public void testDoubleWriteAndCheckSanity() throws Throwable
    {
        createTable("CREATE TABLE %s (p text, v text, PRIMARY KEY (p))");
        execute("INSERT INTO %s(p, v) values (?, ?)", "p1", "v0");
        execute("INSERT INTO %s(p, v) values (?, ?)", "p2", "v0");
        execute("INSERT INTO %s(p, v) values (?, ?)", "p3", "v0");

        SinglePartitionReadCommand readCommand = readCommand("p1", "v");

        assertEquals(1, queryEngine(readCommand).size());
        assertEquals(1, queryCassandraStorage(readCommand).size());

        ColumnFamilyStore cfs = getCurrentColumnFamilyStore();
        RowIteratorSanityCheck.Report report = SanityCheckUtils.checkSanity(cfs);
        assertEquals(report.partitions, 3);
        assertEquals(report.cassandraMissingPartitions, 0);
        assertEquals(report.rocksdbMissingPartitions, 0);
        assertEquals(report.mismatcPartitions, 0);
        assertEquals(report.partitionDeletionMismatch, 0);
        assertEquals(report.rangeTombstoneSkipped, 0);
        assertEquals(report.rows, 3);
        assertEquals(report.cassandraMissingRows, 0);
        assertEquals(report.rocksdbMissingRows, 0);
        assertEquals(report.mismatchRows, 0);
    }
}
