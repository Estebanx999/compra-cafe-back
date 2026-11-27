# compra-cafe

Backend (API REST) del sistema de gestión para un negocio de compraventa de café.

## Arquitectura

Arquitectura hexagonal organizada por **módulos de negocio** (inventario, compras,
ventas, secadero, clientes, finanzas, empleados). Paquete base: `com.compracafe`.

```
modulos/<modulo>/
  domain/
    model/          entidades y reglas de negocio (Java puro)
    repository/     interfaces de los puertos de salida
    exception/      excepciones de negocio del módulo
  application/
    usecase/        casos de uso (clases concretas, sin interfaz)
    dto/            entrada y salida de los casos de uso
  infrastructure/
    persistence/    entidades JPA, repositorios Spring Data, mappers y
                    adaptadores que implementan las interfaces de domain/repository
    web/            controladores REST
shared/             lo que no pertenece a ningún módulo
```

Las carpetas de cada módulo se crean junto con ese módulo, no antes.

### Qué va en cada capa

- **domain**: el modelo y las reglas del negocio. No sabe nada de Spring, de la
  base de datos ni de la web. Cuando necesita guardar o leer algo, declara una
  interfaz en `domain/repository` y nada más.
- **application**: los casos de uso. Orquestan el dominio y reciben y devuelven DTOs.
  Cada caso de uso es una clase concreta, porque tiene una sola implementación.
- **infrastructure**: los detalles técnicos. En `persistence` se implementan las
  interfaces del dominio con JPA, y las entidades `@Entity` son distintas del
  modelo de dominio. En `web` están los controladores, que llaman a los casos de uso.

### Qué puede depender de qué

```
infrastructure ──► application ──► domain
```

| Capa             | Puede usar                                    | No puede usar                                            |
|------------------|-----------------------------------------------|----------------------------------------------------------|
| `domain`         | solo Java y otras clases de `domain`          | Spring, Jakarta Persistence, Hibernate, application, infrastructure |
| `application`    | `domain`                                      | `infrastructure`                                         |
| `infrastructure` | `application`, `domain`, Spring, JPA          | —                                                        |

**Entre módulos:** un módulo solo puede usar la capa `application` de otro módulo
(sus casos de uso y DTOs). Nunca su `domain`, su `domain.repository` ni su
`infrastructure`. Si `ventas` necesita datos de `inventario`, llama a un caso de
uso de `inventario`, nunca a su repositorio.

### Las reglas se verifican solas

`src/test/java/com/compracafe/arquitectura/ArquitecturaTest.java` (ArchUnit)
hace fallar `mvnw test` si se rompe alguna de estas reglas:

1. `domain` no depende de Spring, Jakarta Persistence, Hibernate, `application` ni `infrastructure`.
2. `application` no depende de `infrastructure`.
3. Un módulo solo usa de otro módulo su capa `application`.
4. Los controladores solo están en `infrastructure.web`.
5. Las clases `@Entity` solo están en `infrastructure.persistence`.

Las reglas pasan aunque todavía no haya clases a las que aplicarse, y empiezan
a vigilar en cuanto se crea el primer módulo.
