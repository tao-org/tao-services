package ro.cs.tao.persistence.data.enums;

/**
 * Created by oana on 7/27/2017.
 */
public enum JobStatus {
    RUNNING(1),
    PAUSED(2),
    COMPLETED(3),
    CANCELLED(4),
    ERROR(5),
    PLANNED(6),
    SUSPENDED(7);

    /**
     * Numerical value for enum constants
     */
    private final int value;

    /**
     * Constructor
     * @param s - the integer value identifier
     */
    JobStatus(final int s)
    {
        value = s;
    }

    @Override
    public String toString()
    {
        return String.valueOf(this.value);
    }

    /**
     * Retrieve string enum token corresponding to the integer identifier
     * @param value the integer value identifier
     * @return the string token corresponding to the integer identifier
     */
    public static String getEnumConstantNameByValue(final int value)
    {
        for (JobStatus type : values())
        {
            if ((String.valueOf(value)).equals(type.toString()))
            {
                // return the name of the enum constant having the given value
                return type.name();
            }
        }
        return null;
    }
}
