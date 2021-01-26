import com.acme.labor.entity.Labor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import java.util.UUID

object InMemoryLaborRepository : CrudRepository<UUID, Labor> {

    private var db: MutableList<Labor> = mutableListOf()

    override fun create(value: Labor): Labor {
        db.add(value)
        return value
    }

    override fun findById(key: UUID): Labor? = db.find { c -> c.id == key }

    fun findByName(key: String): Flow<Labor> = db.asFlow().filter { c -> c.name == key }

    override fun update(value: Labor): Labor {
        db.replaceAll { c -> if (c.id == value.id) value else c }
        return value
    }

    override fun deleteById(key: UUID) {
        db.remove(findById(key))
    }

    override fun findAll(): List<Labor> = db
}
