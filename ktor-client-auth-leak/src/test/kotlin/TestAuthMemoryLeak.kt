import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.server.testing.testApplication
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.reflect.Field
import java.util.UUID

class TestAuthMemoryLeak {
    private fun makeClient(
        accessToken: String,
    ): HttpClient {
        return HttpClient(CIO.create()) {
            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(
                            accessToken = accessToken,
                            refreshToken = "",
                        )
                    }
                }
            }
            defaultRequest {
                url("http://www.example.org/")
            }
            expectSuccess = true
        }
    }

    private fun getTokenVersionsValue(instance: Auth.Plugin): Map<*, *>? {
        val field: Field =
            instance::class.memberProperties.find { it.name == "tokenVersions" }?.javaField ?: return null
        field.isAccessible = true
        return field.get(instance) as? Map<*, *>
    }

    @Test
    fun `test memory leak with Auth plugin installation`() = testApplication {
        (1..50).forEach { _ ->
            makeClient(accessToken = UUID.randomUUID().toString()).use {
                it.get { }
            }
        }
        val tokenVersions = getTokenVersionsValue(Auth)
        Assertions.assertTrue(tokenVersions?.isEmpty() == true)
    }
}