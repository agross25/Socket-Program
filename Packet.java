
import java.io.Serializable;

public class Packet implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private final int id;
    private final Type jobType;
    private final String client;

    public Packet(String clientName, int idNum, Type type) {
        client = clientName;
        id = idNum;
        jobType = type;
    }
    
    public String getClient()
    {
        return client;
    }
    
    public int getJobId() {
        return id;
    }

    public Type getJobType() {
        if (jobType == Type.A)
            return Type.A;
        else
            return Type.B;
    }

}
