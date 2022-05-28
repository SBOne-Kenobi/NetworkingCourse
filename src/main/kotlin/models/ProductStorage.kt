package models

object ProductStorage {

    private val data = mutableMapOf<Id, Product>()

    fun snapshot(registered: Boolean = false): List<Product> {
        return data.values.toList().filter { it.isAvailable(registered) }
    }

    fun getById(id: Id, registered: Boolean = false): Product? {
        return data[id]?.let {
            if (it.isAvailable(registered))
                it
            else
                null
        }
    }

    fun add(product: Product) {
        data[product.id] = product
    }

    fun deleteById(id: Id, registered: Boolean = false): Boolean {
        return if (data[id]?.isAvailable(registered) == true) {
            data.remove(id)
            true
        } else {
            false
        }
    }

}