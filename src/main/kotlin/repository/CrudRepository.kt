interface CrudRepository<Key, Value> {
    /**
     * @param value Wert, für den CrudRepositoryerstellt werden soll.
     * @return Erstelltes CrudRepository.
     */
    fun create(value: Value): Value
    /**
     *
     * @param findById Die Id eines Datensatzes.
     * @return Liefert einen Datensatz.
     */
    fun findById(key: Key): Value?
    /**
     *  @return Eine Liste aller Datensätze.
     */
    fun findAll(): List<Value>
    /**
     * @param update Der Datensatz, welcher aktualisiert werden soll.
     * @return Der aktualisierte Datensatz.
     */
    fun update(value: Value): Value
    /**
     * Löscht einen Datensatz.
     * @param deleteById Die Id des zu löschenden Datensatzes.
     */
    fun deleteById(key: Key)
}
