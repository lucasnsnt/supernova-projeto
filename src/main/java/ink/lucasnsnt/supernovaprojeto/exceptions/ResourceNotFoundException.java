package ink.lucasnsnt.supernovaprojeto.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(resource + " não encontrado: " + identifier);
    }
}
