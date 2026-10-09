package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.CANONICAL_GAMES
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("P6692", appName)
    }

    @Test
    fun `canonical games catalogue contains all required games`() {
        val gameIds = CANONICAL_GAMES.map { it.id }
        assertTrue("Aviator game must be present", gameIds.contains("aviator"))
        assertTrue("Fortune Gems 2 game must be present", gameIds.contains("fortune_gems"))
        assertTrue("Money Coming / Spin game must be present", gameIds.contains("money_coming"))
        assertTrue("Cyber Crash must be present", gameIds.contains("cyber_crash"))
        assertTrue("Neon Slots 777 must be present", gameIds.contains("neon_spin"))
        assertTrue("Mines must be present", gameIds.contains("mines"))
        assertTrue("Plinko must be present", gameIds.contains("plinko"))
        assertTrue("Jelly Cluster Rush must be present", gameIds.contains("jelly"))

        // Check categories
        val aviator = CANONICAL_GAMES.first { it.id == "aviator" }
        assertEquals("Crash", aviator.category)
        assertEquals("Aviator: High Ascent", aviator.title)

        val fortuneGems = CANONICAL_GAMES.first { it.id == "fortune_gems" }
        assertEquals("Slots", fortuneGems.category)
        assertEquals("Fortune Gems 2", fortuneGems.title)

        val moneyComing = CANONICAL_GAMES.first { it.id == "money_coming" }
        assertEquals("Arcade", moneyComing.category)
        assertEquals("Money Coming: Spin Wheel", moneyComing.title)
    }

    @Test
    fun `database seeding populates all 17 canonical games on clean install`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

        // Populate initial data
        AppDatabase.populateInitialData(inMemoryDb)

        val allGames = inMemoryDb.gameDao().getAllGames().first()
        assertEquals(CANONICAL_GAMES.size, allGames.size)

        // Verify specific games are active and enabled
        val aviator = inMemoryDb.gameDao().getGameById("aviator")
        assertNotNull(aviator)
        assertTrue(aviator!!.isEnabled)

        val fortuneGems = inMemoryDb.gameDao().getGameById("fortune_gems")
        assertNotNull(fortuneGems)
        assertTrue(fortuneGems!!.isEnabled)

        val moneyComing = inMemoryDb.gameDao().getGameById("money_coming")
        assertNotNull(moneyComing)
        assertTrue(moneyComing!!.isEnabled)

        // Verify initial user
        val user = inMemoryDb.userDao().getUserByIdSync("p6692_user_1")
        assertNotNull(user)
        assertEquals(25000.0, user!!.pkrBalance, 0.01)

        inMemoryDb.close()
    }

    @Test
    fun `category filters correctly partition games`() {
        val crashGames = CANONICAL_GAMES.filter { it.category.equals("Crash", ignoreCase = true) }
        assertTrue(crashGames.any { it.id == "aviator" })
        assertTrue(crashGames.any { it.id == "cyber_crash" })

        val slotGames = CANONICAL_GAMES.filter { it.category.equals("Slots", ignoreCase = true) }
        assertTrue(slotGames.any { it.id == "fortune_gems" })
        assertTrue(slotGames.any { it.id == "neon_spin" })
        assertTrue(slotGames.any { it.id == "jelly" })

        val arcadeGames = CANONICAL_GAMES.filter { it.category.equals("Arcade", ignoreCase = true) }
        assertTrue(arcadeGames.any { it.id == "money_coming" })
        assertTrue(arcadeGames.any { it.id == "bubble_shooter" })
        assertTrue(arcadeGames.any { it.id == "candy_match" })
        assertTrue(arcadeGames.any { it.id == "fruit_merge" })
    }
}
