package org.gwtproject.rpc.serial.processor;

import com.google.common.base.Preconditions;

import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Wraps two serializable type oracles, using one as "read" and one as "write", allowing
 * questions to be asked about which directions a type could be sent
 */
public class SerializableTypeOracleUnion implements SerializableTypeOracle {

    private final SerializableTypeOracle read;
    private final SerializableTypeOracle write;

    public SerializableTypeOracleUnion(SerializableTypeOracle read, SerializableTypeOracle write) {
        this.read = read;
        this.write = write;
    }

    @Override
    public Set<TypeMirror> getSerializableTypes() {
        TreeSet<TypeMirror> all = new TreeSet<>(new TypeMirrorNameComparator());
        all.addAll(read.getSerializableTypes());
        all.addAll(write.getSerializableTypes());
        return Collections.unmodifiableSet(all);
    }

    @Override
    public boolean isSerializable(TypeMirror type) {
        return Stream.of(read, write).anyMatch(oracle -> oracle.isSerializable(type));
    }

    @Override
    public boolean maybeInstantiated(TypeMirror type) {
        return Stream.of(read, write).anyMatch(oracle -> oracle.maybeInstantiated(type));
    }

    /**
     * Gets the specific subclass, with just the ability to serialize, instantiate, deserialize based on the contained oracles
     * @param type
     * @return
     */
    public String getSpecificFieldSerializer(TypeMirror type) {
        boolean canSerialize = write.isSerializable(type);
        boolean canDeserialize = read.isSerializable(type);
        boolean canInstantiate = read.maybeInstantiated(type);

        //TODO this is terrible, factor it out nicely, and share logic/naming with
        //Processor.writeInstanceMethods()

        if (canSerialize) {
            if (canDeserialize) {
                if (canInstantiate) {
                    return "WriteInstantiateReadInstantiate";
                } else {
                    return "WriteInstantiateReadSuperclass";
                }
            } else {
//                assert !canInstantiate : "Can instantiate, but not deserialize " + type;
                Preconditions.checkState(!canInstantiate, "Can instantiate, but not deserialize " + type);
                return "WriteOnly";
            }
        } else {
            if (canDeserialize) {
                if (canInstantiate) {
                    return "ReadOnlyInstantiate";
                } else {
                    return "ReadOnlySuperclass";
                }
            } else {
                Preconditions.checkState(false, "Can't serialize or deserialize " + type);
//                assert false : "can't serialize or deserialize!";
            }
        }

        throw new RuntimeException("something is broken, run with asserts");
    }

}
