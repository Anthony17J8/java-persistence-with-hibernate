@org.hibernate.annotations.NamedQueries({
        @org.hibernate.annotations.NamedQuery(
                name = "findItemsOrderByName",
                query = "SELECT i FROM Item i ORDER BY i.name DESC"
        )
})
package com.ico.ltd.querying.domain;