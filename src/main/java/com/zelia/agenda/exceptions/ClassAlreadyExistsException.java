package com.zelia.agenda.exceptions;

public class ClassAlreadyExistsException extends RuntimeException {
    public ClassAlreadyExistsException(String name) {
        super("Já existe uma turma com o nome: " + name);
    }
}