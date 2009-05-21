package org.infinispan.marshall.jboss.externalizers;

import org.infinispan.remoting.responses.UnsuccessfulResponse;
import org.jboss.marshalling.Creator;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Externalizes an UnsuccessfulResponse
 *
 * @author Manik Surtani
 * @since 4.0
 * @deprecated Not needed UnsuccessfulResponseExternalizer, it's more 
 * performant adding singleton instance to ObjectTable, see 
 * RequestIgnoredResponse example in CustomObjectTable
 */
@Deprecated
public class UnsuccessfulResponseExternalizer implements Externalizer {
   public void writeExternal(Object o, ObjectOutput objectOutput) throws IOException {
   }

   public Object createExternal(Class<?> aClass, ObjectInput objectInput, Creator creator) throws IOException, ClassNotFoundException {
      return UnsuccessfulResponse.INSTANCE;
   }

   public void readExternal(Object o, ObjectInput objectInput) throws IOException, ClassNotFoundException {
   }
}
