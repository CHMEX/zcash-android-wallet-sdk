package cash.z.wallet.sdk.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.RoomDatabase
import cash.z.wallet.sdk.entity.*

//
// Database
//

@Database(
    entities = [
        TransactionEntity::class,
        Block::class,
        Received::class,
        Account::class,
        Sent::class
    ],
    version = 3,
    exportSchema = false
)
abstract class DerivedDataDb : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun blockDao(): BlockDao
    abstract fun receivedDao(): ReceivedDao
    abstract fun sentDao(): SentDao
}


//
// Data Access Objects
//

@Dao
interface BlockDao {
    @Query("SELECT COUNT(height) FROM blocks")
    fun count(): Int

    @Query("SELECT MAX(height) FROM blocks")
    fun lastScannedHeight(): Int
}

@Dao
interface ReceivedDao {
    @Query("SELECT COUNT(tx) FROM received_notes")
    fun count(): Int
}

@Dao
interface SentDao {
    @Query("SELECT COUNT(tx) FROM sent_notes")
    fun count(): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT COUNT(id_tx) FROM transactions")
    fun count(): Int

    @Query("SELECT COUNT(block) FROM transactions WHERE block IS NULL")
    fun countUnmined(): Int

    @Query("""
        SELECT transactions.txid AS txId, 
               transactions.raw  AS raw 
        FROM   transactions
        WHERE  id_tx = :id AND raw is not null
        """)
    fun findEncodedTransactionById(id: Long): EncodedTransaction?

    @Query("""
        SELECT transactions.block
        FROM   transactions 
        WHERE  txid = :rawTransactionId
        LIMIT  1 
    """)
    fun findMinedHeight(rawTransactionId: ByteArray): Int?

//    @Delete
//    fun delete(transaction: Transaction)
//
//    @Query("DELETE FROM transactions WHERE id_tx = :id")
//    fun deleteById(id: Long)

    /**
     * Query sent transactions that have been mined, sorted so the newest data is at the top.
     */
    @Query("""
        SELECT transactions.id_tx         AS id,
               transactions.block         AS minedHeight,
               transactions.tx_index      AS transactionIndex,
               transactions.txid          AS rawTransactionId,
               transactions.expiry_height AS expiryHeight,
               transactions.raw           AS raw,
               sent_notes.address         AS toAddress,
               sent_notes.value           AS value,
               sent_notes.memo            AS memo,
               sent_notes.id_note         AS noteId,
               blocks.time                AS blockTimeInSeconds
        FROM   transactions
               LEFT JOIN sent_notes 
                      ON transactions.id_tx = sent_notes.tx
               LEFT JOIN blocks
                      ON transactions.block = blocks.height
        WHERE  transactions.raw IS NOT NULL
               AND minedheight > 0
        ORDER  BY block IS NOT NULL, height DESC, time DESC, txid DESC
        LIMIT  :limit
    """)
    fun getSentTransactions(limit: Int = Int.MAX_VALUE): DataSource.Factory<Int, ConfirmedTransaction>


    /**
     * Query transactions, aggregating information on send/receive, sorted carefully so the newest
     * data is at the top and the oldest transactions are at the bottom.
     */
    @Query("""
        SELECT transactions.id_tx     AS id,
               transactions.block     AS minedHeight,
               transactions.tx_index  AS transactionIndex,
               transactions.txid      AS rawTransactionId,
               received_notes.value   AS value,
               received_notes.memo    AS memo,
               received_notes.id_note AS noteId,
               blocks.time            AS blockTimeInSeconds
        FROM   transactions
               LEFT JOIN received_notes
                      ON transactions.id_tx = received_notes.tx
               LEFT JOIN blocks
                      ON transactions.block = blocks.height
        WHERE  received_notes.is_change != 1
        ORDER  BY minedheight DESC, blocktimeinseconds DESC, id DESC
        LIMIT  :limit
    """)
    fun getReceivedTransactions(limit: Int = Int.MAX_VALUE): DataSource.Factory<Int, ConfirmedTransaction>

    @Query("""
         SELECT transactions.id_tx          AS id,
               transactions.block           AS minedHeight,
               transactions.tx_index        AS transactionIndex,
               transactions.txid            AS rawTransactionId,
               transactions.expiry_height   AS expiryHeight,
               transactions.raw             AS raw,
               sent_notes.address           AS toAddress,
               CASE
                 WHEN transactions.raw IS NOT NULL THEN sent_notes.value
                 ELSE received_notes.value
               end                          AS value,
               CASE
                 WHEN transactions.raw IS NOT NULL THEN sent_notes.memo
                 ELSE received_notes.memo
               end                          AS memo,
               CASE
                 WHEN transactions.raw IS NOT NULL THEN sent_notes.id_note
                 ELSE received_notes.id_note
               end                          AS noteId,
               blocks.time                  AS blockTimeInSeconds
         FROM   transactions
               LEFT JOIN received_notes
                      ON transactions.id_tx = received_notes.tx
               LEFT JOIN sent_notes
                      ON transactions.id_tx = sent_notes.tx
               LEFT JOIN blocks
                      ON transactions.block = blocks.height
         WHERE  ( transactions.raw IS NULL
                 AND received_notes.is_change != 1 )
                OR ( transactions.raw IS NOT NULL )
         ORDER  BY ( minedheight IS NOT NULL ),
                  minedheight DESC,
                  blocktimeinseconds DESC,
                  id DESC  
         LIMIT  :limit
    """)
    fun getAllTransactions(limit: Int = Int.MAX_VALUE): DataSource.Factory<Int, ConfirmedTransaction>
}