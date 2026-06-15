(function () {
    var storageKey = "eshop.catalog.basket";
    var ordersKey = "eshop.catalog.orders";
    var currentOrderKey = "eshop.catalog.currentOrderId";
    var nextOrderKey = "eshop.catalog.nextOrderNumber";
    var currency = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

    function readBasket() {
        try {
            var parsed = JSON.parse(window.localStorage.getItem(storageKey));
            return Array.isArray(parsed) ? parsed : [];
        } catch (error) {
            return [];
        }
    }

    function saveBasket(items) {
        window.localStorage.setItem(storageKey, JSON.stringify(items));
    }

    function readOrders() {
        try {
            var parsed = JSON.parse(window.localStorage.getItem(ordersKey));
            return Array.isArray(parsed) ? parsed : [];
        } catch (error) {
            return [];
        }
    }

    function saveOrders(orders) {
        window.localStorage.setItem(ordersKey, JSON.stringify(orders));
    }

    function nextOrderNumber() {
        var next = Number.parseInt(window.localStorage.getItem(nextOrderKey) || "1", 10);
        window.localStorage.setItem(nextOrderKey, String(next + 1));
        return next;
    }

    function priceFrom(value) {
        var price = Number.parseFloat(String(value || "").replace(/[^0-9.-]/g, ""));
        return Number.isFinite(price) ? price : 0;
    }

    function addToBasket(product) {
        var items = readBasket();
        var id = product.id || product.name;
        var existing = items.find(function (item) {
            return item.id === id;
        });

        if (existing) {
            existing.quantity += 1;
        } else {
            items.push({
                id: id,
                name: product.name,
                price: priceFrom(product.price),
                quantity: 1,
                picture: product.picture || ""
            });
        }

        saveBasket(items);
        window.localStorage.removeItem(currentOrderKey);
        renderBasket(items);
    }

    function changeQuantity(id, delta) {
        var items = readBasket();
        var item = items.find(function (candidate) {
            return candidate.id === id;
        });

        if (!item) {
            return;
        }

        item.quantity += delta;
        if (item.quantity <= 0) {
            items = items.filter(function (candidate) {
                return candidate.id !== id;
            });
        }

        saveBasket(items);
        window.localStorage.removeItem(currentOrderKey);
        renderBasket(items);
    }

    function renderBasket(items) {
        var basket = document.querySelector("[data-basket]");
        if (!basket) {
            renderPaymentSummary(items);
            return;
        }

        var list = basket.querySelector("[data-basket-items]");
        var empty = basket.querySelector("[data-basket-empty]");
        var total = basket.querySelector("[data-basket-total]");
        var itemCount = items.reduce(function (sum, item) {
            return sum + item.quantity;
        }, 0);
        var basketTotal = items.reduce(function (sum, item) {
            return sum + item.price * item.quantity;
        }, 0);

        var count = basket.querySelector("[data-basket-count]");
        if (count) {
            count.textContent = "(" + itemCount + ")";
        }
        total.textContent = currency.format(basketTotal);
        empty.hidden = items.length > 0;
        list.innerHTML = items.map(function (item) {
            var id = escapeHtml(item.id);
            var name = escapeHtml(item.name);
            return [
                "<li class='esh-basket-line'>",
                "<h3>", name, "</h3>",
                "<p>", currency.format(item.price), " each</p>",
                "<div class='esh-basket-item-quantity' aria-label='Quantity'>",
                "<button class='esh-basket-quantity-button' type='button' data-basket-action='decrease' data-basket-id='", id, "' aria-label='Decrease ", name, " quantity'>-</button>",
                "<span class='esh-basket-quantity-value'>", item.quantity, "</span>",
                "<button class='esh-basket-quantity-button' type='button' data-basket-action='increase' data-basket-id='", id, "' aria-label='Increase ", name, " quantity'>+</button>",
                "</div>",
                "</li>"
            ].join("");
        }).join("");
        renderPaymentSummary(items);
        renderOrders();
    }

    function renderPaymentSummary(items) {
        var paymentTotal = document.querySelector("[data-payment-total]");
        var orderItems = document.querySelector("[data-order-items]");
        var orderNumber = document.querySelector("[data-order-number]");
        if (!paymentTotal && !orderItems && !orderNumber) {
            return;
        }

        var order = ensureCurrentOrder(items);
        var orderSource = order || buildOrderPreview(items);
        if (paymentTotal) {
            paymentTotal.textContent = currency.format(orderSource.total);
        }
        if (orderNumber) {
            orderNumber.textContent = orderSource.number ? "Order #" + orderSource.number : "Order";
        }

        var submitted = document.querySelector("[data-order-submitted]");
        if (submitted) {
            submitted.textContent = orderSource.submittedAt
                ? "Submitted - " + new Date(orderSource.submittedAt).toLocaleString()
                : "Submitted";
        }

        if (orderItems) {
            orderItems.innerHTML = orderSource.items.map(function (item) {
                return [
                    "<li>",
                    "<span>", escapeHtml(item.name), " x ", item.quantity, "</span>",
                    "<strong>", currency.format(item.price * item.quantity), "</strong>",
                    "</li>"
                ].join("");
            }).join("");
        }
    }

    function buildOrderPreview(items) {
        return {
            number: "",
            submittedAt: "",
            items: items.map(copyOrderItem),
            total: totalFor(items)
        };
    }

    function ensureCurrentOrder(items) {
        if (!items.length) {
            return null;
        }

        var orders = readOrders();
        var currentId = window.localStorage.getItem(currentOrderKey);
        var order = orders.find(function (candidate) {
            return candidate.id === currentId && candidate.status !== "Paid";
        });

        if (!order) {
            var number = nextOrderNumber();
            order = {
                id: String(number),
                number: number,
                status: "Submitted",
                submittedAt: new Date().toISOString(),
                items: [],
                total: 0
            };
            orders.push(order);
            window.localStorage.setItem(currentOrderKey, order.id);
        }

        order.items = items.map(copyOrderItem);
        order.total = totalFor(items);
        saveOrders(orders);
        return order;
    }

    function completePayment() {
        var items = readBasket();
        var order = ensureCurrentOrder(items);
        if (order) {
            var orders = readOrders();
            var persisted = orders.find(function (candidate) {
                return candidate.id === order.id;
            });
            if (persisted) {
                persisted.status = "Paid";
                persisted.paidAt = new Date().toISOString();
                persisted.items = items.map(copyOrderItem);
                persisted.total = totalFor(items);
                saveOrders(orders);
            }
        }

        saveBasket([]);
        window.localStorage.removeItem(currentOrderKey);
    }

    function renderOrders() {
        var list = document.querySelector("[data-orders-list]");
        var empty = document.querySelector("[data-orders-empty]");
        if (!list || !empty) {
            return;
        }

        var orders = readOrders().filter(function (order) {
            return order.status;
        });
        empty.hidden = orders.length > 0;
        list.innerHTML = orders.map(function (order) {
            return [
                "<li>",
                "<span>Order #", order.number, "</span>",
                "<strong>", escapeHtml(order.status), "</strong>",
                "</li>"
            ].join("");
        }).join("");
    }

    function copyOrderItem(item) {
        return {
            id: item.id,
            name: item.name,
            price: priceFrom(item.price),
            quantity: item.quantity
        };
    }

    function totalFor(items) {
        return items.reduce(function (sum, item) {
            return sum + priceFrom(item.price) * item.quantity;
        }, 0);
    }

    function escapeHtml(value) {
        return String(value || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    document.addEventListener("click", function (event) {
        var submitPayment = event.target.closest("[data-submit-payment]");
        if (submitPayment) {
            event.preventDefault();
            completePayment();
            window.location.href = submitPayment.href;
            return;
        }

        var basketButton = event.target.closest("[data-basket-action]");
        if (basketButton) {
            event.preventDefault();
            changeQuantity(basketButton.dataset.basketId, basketButton.dataset.basketAction === "increase" ? 1 : -1);
            return;
        }

        var addButton = event.target.closest("[data-add-to-basket]");
        if (!addButton) {
            return;
        }

        event.preventDefault();
        addToBasket({
            id: addButton.dataset.productId,
            name: addButton.dataset.productName,
            price: addButton.dataset.productPrice,
            picture: addButton.dataset.productPicture
        });
    });

    document.addEventListener("DOMContentLoaded", function () {
        var items = readBasket();
        renderBasket(items);
        renderPaymentSummary(items);
        renderOrders();
    });
}());
