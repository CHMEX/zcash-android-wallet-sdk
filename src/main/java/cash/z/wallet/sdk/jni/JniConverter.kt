package cash.z.wallet.sdk.jni

class JniConverter {

    external fun initDataDb(dbData: String): Boolean

    external fun initAccountsTable(
        dbData: String,
        seed: ByteArray,
        accounts: Int): Boolean

    external fun initBlocksTable(
        dbData: String,
        height: Int,
        time: Long,
        saplingTree: ByteArray): Boolean

    external fun getAddress(dbData: String, account: Int): String

    external fun getBalance(dbData: String): Long

    external fun scanBlocks(db_cache: String, db_data: String): Boolean

    external fun sendToAddress(
        dbData: String,
        seed: ByteArray,
        to: String,
        value: Long,
        memo: String,
        spendParams: String,
        outputParams: String): Long

    external fun initLogs()

    companion object {
        init {
            System.loadLibrary("zcashwalletsdk")
        }
    }

}