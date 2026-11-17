package id.my.chromazone.leetify_widget.ui

import org.junit.Assert.*
import org.junit.Test

class AccountInputTest {
    @Test fun validatesIdAndOptionalKeyWithoutChangingTheirValues() {
        assertNull(validateAccountInput("76561190000000000", ""))
        assertNull(validateAccountInput("76561190000000000", "synthetic-key"))
        for (id in listOf("123", "7656119000000000a", "７６５６１１９００００００００００")) {
            assertEquals(AccountMessage.INVALID_ID, validateAccountInput(id, ""))
        }
        for (key in listOf("bad\nkey", "bad key", "\u0000")) {
            assertEquals(AccountMessage.INVALID_KEY_FORMAT, validateAccountInput("76561190000000000", key))
        }
        assertFalse(AccountState(apiKey = "synthetic-secret").toString().contains("synthetic-secret"))
    }
}
