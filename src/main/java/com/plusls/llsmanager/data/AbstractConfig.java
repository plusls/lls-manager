package com.plusls.llsmanager.data;

import com.google.gson.JsonParseException;
import com.plusls.llsmanager.LlsManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractConfig<T> {
    protected final Path path;

    private final Class<T> dataType;

    public AbstractConfig(Path path, Class<T> dataType) {
        this.path = path;
        this.dataType = dataType;
    }

    protected abstract T getData();


    protected abstract void setData(T data);

    public boolean save() {
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
                LlsManager.logger().error("Save {} error: createFile fail.", path);
                return false;
            }
        }
        BufferedWriter bfw;
        try {
            bfw = Files.newBufferedWriter(path);
        } catch (IOException e) {
            e.printStackTrace();
            LlsManager.logger().error("Save {} error: newBufferedWriter fail.", path);
            return false;
        }

        try {
            bfw.write(LlsManager.GSON.toJson(getData()));
            bfw.close();
        } catch (IOException e) {
            e.printStackTrace();
            LlsManager.logger().error("Save {} error: bfw.write fail.", path);
            return false;
        }
        return true;
    }

    public boolean load() {
        if (!Files.exists(path)) {
            return save();
        }
        try {
            BufferedReader bfr = Files.newBufferedReader(path);
            setData(LlsManager.GSON.fromJson(bfr, dataType));
            bfr.close();
        } catch (IOException e) {
            e.printStackTrace();
            LlsManager.logger().error("Load {} error: newBufferedReader fail.", path);
            return false;
        } catch (JsonParseException e) {
            LlsManager.logger().error("Json {} parser fail!!", path);
            return false;
        }
        return true;
    }
}
