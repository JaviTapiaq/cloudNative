package com.pedidos360.productos.service;

import com.pedidos360.productos.model.Producto;
import com.pedidos360.productos.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    public List<Producto> findAll() {
        return repository.findAll();
    }

    public Optional<Producto> findById(Long id) {
        return repository.findById(id);
    }

    public List<Producto> findByCategoria(String categoria) {
        return repository.findByCategoria(categoria);
    }

    public Producto save(Producto producto) {
        return repository.save(producto);
    }

    public Optional<Producto> update(Long id, Producto producto) {
        return repository.findById(id).map(existing -> {
            existing.setNombre(producto.getNombre());
            existing.setDescripcion(producto.getDescripcion());
            existing.setPrecio(producto.getPrecio());
            existing.setStock(producto.getStock());
            existing.setCategoria(producto.getCategoria());
            return repository.save(existing);
        });
    }

    public boolean delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}
