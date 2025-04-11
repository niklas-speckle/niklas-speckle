package at.qe.skeleton.rest.mapper;

/* W4: demo general DTO mapper */

public interface DTOMapper<E, D> {

    D mapTo(E entity);
    E mapFrom(D dto);
}
