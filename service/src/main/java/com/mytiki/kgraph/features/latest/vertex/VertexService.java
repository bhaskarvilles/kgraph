/*
 * Copyright (c) TIKI Inc.
 * MIT license. See LICENSE file in root directory.
 */

package com.mytiki.kgraph.features.latest.vertex;

import com.arangodb.ArangoDBException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytiki.common.exception.ApiExceptionBuilder;
import org.springframework.http.HttpStatus;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

public class VertexService {
    private final ObjectMapper objectMapper;
    private final VertexLookup vertexLookup;

    public VertexService(VertexLookup vertexLookup, ObjectMapper objectMapper) {
        this.vertexLookup = vertexLookup;
        this.objectMapper = objectMapper;
    }

    public List<?> schema(){
        String schema = vertexLookup.getSchema();
        try {
            return objectMapper.readValue(schema, List.class);
        } catch (JsonProcessingException e) {
            throw new ApiExceptionBuilder()
                    .httpStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                    .build();
        }
    }

    public <T extends VertexDO> Optional<T> get(String type, String id){
        VertexRepository<T> repository = getRepository(type);
        if(repository == null) return Optional.empty();
        try {
            return repository.findById(id);
        }catch (ArangoDBException ex){
            if(ex.getErrorNum() == 1203) return Optional.empty();
            else throw ex;
        }
    }

    public <T extends VertexDO> T upsert(T vertex){
        VertexRepository<T> repository = getRepository(vertex.getCollection());
        return repository.upsert(vertex);
    }

    public <T extends VertexDO> List<T> insert (List<T> vertices){
        VertexRepository<T> repository = getRepository(vertices.get(0).getCollection());
        return repository.insertAll(vertices);
    }

    @SuppressWarnings("unchecked")
    public <T extends VertexDO> T fromType(String type)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<T> doClass = (Class<T>) vertexLookup.getDOClass(type);
        if(doClass == null) throw new NoSuchMethodException();
        return doClass.getConstructor().newInstance();
    }

    @SuppressWarnings("unchecked")
    private <T extends VertexDO> VertexRepository<T> getRepository(String type){
        return (VertexRepository<T>) vertexLookup.getRepository(type);
    }
}
