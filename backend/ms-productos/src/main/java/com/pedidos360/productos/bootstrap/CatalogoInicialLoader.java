package com.pedidos360.productos.bootstrap;

import com.pedidos360.productos.model.Producto;
import com.pedidos360.productos.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Inserta un catalogo de ejemplo solo si la tabla de productos esta vacia, para que la
 * tienda tenga datos desde el primer arranque (tanto en H2 como en Supabase).
 */
@Component
@ConditionalOnProperty(prefix = "productos.catalogo-inicial", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CatalogoInicialLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogoInicialLoader.class);

    private final ProductoRepository repository;

    public CatalogoInicialLoader(ProductoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("Catalogo existente con {} producto(s); no se cargan datos iniciales", repository.count());
            return;
        }
        List<Producto> catalogo = List.of(
                producto("Notebook Lenovo IdeaPad 3", "15.6\", Ryzen 5, 16 GB RAM, 512 GB SSD", "449990", 10, "Computacion"),
                producto("Monitor Samsung 24\"", "Full HD IPS, 75 Hz", "129990", 8, "Computacion"),
                producto("Mouse inalambrico Logitech M170", "Receptor USB, 12 meses de bateria", "12990", 50, "Accesorios"),
                producto("Teclado mecanico Redragon Kumara", "Switches red, retroiluminado", "39990", 25, "Accesorios"),
                producto("Audifonos Sony WH-CH520", "Bluetooth, 50 horas de bateria", "44990", 30, "Audio"),
                producto("Silla ergonomica Ergo Pro", "Soporte lumbar ajustable", "159990", 3, "Oficina")
        );
        repository.saveAll(catalogo);
        log.info("Catalogo inicial cargado: {} productos", catalogo.size());
    }

    private Producto producto(String nombre, String descripcion, String precio, int stock, String categoria) {
        return new Producto(null, nombre, descripcion, new BigDecimal(precio), stock, categoria);
    }
}
