package model.timeline.array;

import control.threads.ThreadController;

/**
 * Container holding the measurement timeline values for the samples taken in
 * pipes.
 *
 * @author saemann
 */
public class ArrayTimeLineMeasurementContainer {

    public static ArrayTimeLineMeasurementContainer instance;

    private TimeContainer times;
    public int[] particles;
    /**
     * Mass of contaminants in total [timeindex]
     */
    public float[] mass_total;
    /**
     * mass of different types of contaminants [timeindex][contaminantIndex]
     */
    public float[][] mass_type;
    public int[] particles_visited;
    public float[] volumes;
    public int[] counts;
    public long[] measurementTimes;

    /**
     * Samples are only taken if this is true. Can be switched of, to save
     * computation cost. SYnchronisation Thread switches this flag on and off.
     */
    public boolean measurementsActive = true;
    /**
     * Number of samples in this timeinterval. Important if the global number
     * does not fit the interval number. E.g in the very first and very last
     * interval;
     */
    public int[] samplesInTimeInterval;

    /**
     * Is it only recorded once per timeindex?
     */
    private boolean timespotmeasurement = false;

    /**
     * Indicates how many samples are taken during one sampling interval. This
     * variable is only for debugging and is not used for the calculation.
     */
    public double samplesPerTimeinterval = 1;
    /**
     * Distance from an injection point to calculate the momentum of
     * concentration.
     */
    public static float[] distance;

    private int actualTimeIndex = 0;
    private int numberOfCapacities;
    private int numberOfContaminants;

    public static double maxConcentration_global = 0;

    public static boolean isInitialized() {
        return instance != null;
    }

    public static ArrayTimeLineMeasurementContainer init(long[] times, int numberOfPipes, int numberOfContaminantTypes) {
        return init(new TimeContainer(times), numberOfPipes, numberOfContaminantTypes);
    }

    public static ArrayTimeLineMeasurementContainer init(TimeContainer times, int numberOfPipes, int numberOfContaminantTypes) {
        ArrayTimeLineMeasurementContainer container = new ArrayTimeLineMeasurementContainer(times, numberOfPipes, numberOfContaminantTypes);
        instance = container;
        return container;
    }

    public ArrayTimeLineMeasurementContainer(long[] times, int numberOfCapacities, int numberOfContaminantTypes) {
        this(new TimeContainer(times), numberOfCapacities, numberOfContaminantTypes);
    }

    public ArrayTimeLineMeasurementContainer(TimeContainer times, int numberOfPipes, int numberOfContaminantTypes) {
        this.times = times;
        initialize(times.getNumberOfTimes(), numberOfPipes, numberOfContaminantTypes);
    }

    private void initialize(int numberOfTimes, int numberOfPipes, int numberOfContaminantTypes) {
        this.numberOfCapacities = numberOfPipes;
        this.numberOfContaminants = numberOfContaminantTypes;
        this.counts = new int[numberOfPipes * numberOfTimes];
        this.particles = new int[numberOfPipes * numberOfTimes];
        this.particles_visited = new int[numberOfPipes * numberOfTimes];
        this.volumes = new float[numberOfPipes * numberOfTimes];
        this.mass_total = new float[numberOfPipes * numberOfTimes];

        this.mass_type = new float[numberOfPipes * numberOfTimes][numberOfContaminantTypes];
        this.measurementTimes = new long[numberOfTimes];
        this.samplesInTimeInterval = new int[numberOfTimes];
    }

    public void setNumberOfMaterials(int number) {
        if (this.numberOfContaminants == number) {
            return;
        }
        this.numberOfContaminants = number;
        this.mass_type = new float[numberOfCapacities * times.getNumberOfTimes()][number];
    }

    public void setActualTime(long time) {
        int neuerIndex = getIndexForTime(time);
        actualTimeIndex = neuerIndex;
    }

    public int getActualTimeIndex() {
        return actualTimeIndex;
    }

    public void OnlyRecordOncePerTimeindex() {
        this.timespotmeasurement = true;
        this.samplesPerTimeinterval = 1;
    }

    public void setSamplesPerTimeindex(double recordsPerTimeindex) {
        this.timespotmeasurement = false;
        this.samplesPerTimeinterval = recordsPerTimeindex;
    }

    public void setIntervalSeconds(double seconds, long startTime, long endTime) {
        if (seconds == this.getDeltaTimeS()) {
            //Nothing changed
            return;
        }
        //Create timecontainer
        double oldduration = (endTime - startTime) / 1000.;
        int numberOfTimes = (int) (oldduration / seconds + 1);
        long[] t = new long[numberOfTimes];
        for (int i = 0; i < t.length; i++) {
            t[i] = (long) (startTime + i * seconds * 1000);
        }
        TimeContainer tc = new TimeContainer(t);
        samplesPerTimeinterval = (tc.getDeltaTimeMS() / 1000.) / ThreadController.getDeltaTime();
        times = tc;
        initialize(numberOfTimes, numberOfCapacities, numberOfContaminants);
    }

    /**
     * Seconds per Timeindex between storing timesteps.
     *
     * @return seconds
     */
    public double getDeltaTimeS() {
        return times.getDeltaTimeMS() / 1000.;
    }

    public double getMomentum1_xc(int timeIndex) {
        double zaehler = 0;
        double nenner = 0;
        double c;
        int index = 0;
//        int mittlungsradius = 5;
        int to = Math.min(distance.length, numberOfCapacities);
        for (int i = 0; i < to; i++) {
            try {
                index = i * times.getNumberOfTimes() + timeIndex;
                if (particles[index] < 1) {
                    continue;
                }
                c = (((double) mass_total[index] * (double) counts[index]) / (volumes[index] * samplesInTimeInterval[timeIndex]/*samplesPerTimeinterval*/));

                zaehler += (distance[i]) * c;
                nenner += c;
            } catch (Exception e) {
                System.out.println("timeIndex: " + timeIndex + "  index=" + (i * times.getNumberOfTimes() + timeIndex) + "  i=" + i + " times.length=" + times.getNumberOfTimes());
                System.out.println("distance[" + i + "]=" + distance[i]);
                System.out.println("particles.length=" + particles.length);
                System.out.println("particles[" + index + "]=" + particles[index]);
                e.printStackTrace();
                break;
            }
        }
        return zaehler / nenner;
    }

    public double getMomentum1_xm(int timeIndex) {
        double zaehler = 0;
        double nenner = 0;
        double m;
        int index = 0;
//        int mittlungsradius = 5;
        int to = Math.min(distance.length, numberOfCapacities);
        for (int i = 0; i < to; i++) {
            try {
                index = i * times.getNumberOfTimes() + timeIndex;
                if (particles[index] < 1) {
                    continue;
                }
                m = (((double) mass_total[index] * (double) counts[index]) / (samplesInTimeInterval[timeIndex]/*samplesPerTimeinterval*/));

                zaehler += (distance[i]) * m;
                nenner += m;
            } catch (Exception e) {
                System.out.println("timeIndex: " + timeIndex + "  index=" + (i * times.getNumberOfTimes() + timeIndex) + "  i=" + i + " times.length=" + times.getNumberOfTimes());
                System.out.println("distance[" + i + "]=" + distance[i]);
                System.out.println("particles.length=" + particles.length);
                System.out.println("particles[" + index + "]=" + particles[index]);
                e.printStackTrace();
                break;
            }
        }
        return zaehler / nenner;
    }

    public double getMomentum2_xc(int timeIndex, double moment1) {
        double zaehler = 0;
        double nenner = 0;
        double c;
        int index = 0;
        int to = Math.min(distance.length, numberOfCapacities);
        for (int i = 0; i < to; i++) {
            try {
                index = i * times.getNumberOfTimes() + timeIndex;

                if (particles[index] < 1) {
                    continue;
                }
//                //ArrayTimeLinePipe.concentration_reference[index];//
                c = (((double) mass_total[index] * (double) counts[index]) / (volumes[index] * samplesInTimeInterval[timeIndex]/*samplesPerTimeinterval*/));

                zaehler += (distance[i] - moment1) * (distance[i] - moment1) * c;
                nenner += c;
            } catch (Exception e) {
                System.out.println("timeIndex: " + timeIndex + "  index=" + (i * times.getNumberOfTimes() + timeIndex) + "  i=" + i + " times.length=" + times.getNumberOfTimes());
                System.out.println("distance[" + i + "]=" + distance[i]);
                System.out.println("particles.length=" + particles.length);
                System.out.println("particles[" + index + "]=" + particles[index]);
                e.printStackTrace();
                break;
            }
        }
        return zaehler / nenner;
    }

    public double getMomentum2_xm(int timeIndex, double moment1xm) {
        double zaehler = 0;
        double nenner = 0;
        double m;
        int index = 0;
        int to = Math.min(distance.length, numberOfCapacities);
        for (int i = 0; i < to; i++) {
            try {
                index = i * times.getNumberOfTimes() + timeIndex;

                if (particles[index] < 1) {
                    continue;
                }
//                //ArrayTimeLinePipe.concentration_reference[index];//
                m = (((double) mass_total[index] * (double) counts[index]) / (samplesInTimeInterval[timeIndex]/*samplesPerTimeinterval*/));

                zaehler += (distance[i] - moment1xm) * (distance[i] - moment1xm) * m;
                nenner += m;
            } catch (Exception e) {
                System.out.println("timeIndex: " + timeIndex + "  index=" + (i * times.getNumberOfTimes() + timeIndex) + "  i=" + i + " times.length=" + times.getNumberOfTimes());
                System.out.println("distance[" + i + "]=" + distance[i]);
                System.out.println("particles.length=" + particles.length);
                System.out.println("particles[" + index + "]=" + particles[index]);
                e.printStackTrace();
                break;
            }
        }
        return zaehler / nenner;
    }

    public long getTimeMillisecondsAtIndex(int timeIndex) {
        return times.getTimeMilliseconds(timeIndex);
    }

    public int getIndexForTime(long time) {
        return times.getTimeIndex(time);
    }

    public int getNumberOfTimes() {
        return times.getNumberOfTimes();
    }

    public long getStartTime() {
        return times.getTimeMilliseconds(0);
    }

    public long getEndTime() {
        return times.getTimeMilliseconds(times.getNumberOfTimes() - 1);
    }

    public void clearValues() {

        this.counts = new int[numberOfCapacities * times.getNumberOfTimes()];
        this.particles = new int[numberOfCapacities * times.getNumberOfTimes()];
        this.particles_visited = new int[numberOfCapacities * times.getNumberOfTimes()];
        this.volumes = new float[numberOfCapacities * times.getNumberOfTimes()];
        this.mass_total = new float[numberOfCapacities * times.getNumberOfTimes()];

        this.mass_type = new float[numberOfCapacities * times.getNumberOfTimes()][numberOfContaminants];
        this.samplesInTimeInterval = new int[times.getNumberOfTimes()];

        maxConcentration_global = 0;
    }

    public float[] getMassForTimeIndex(int timeIndex) {
        float[] r = new float[distance.length];
        for (int i = 0; i < distance.length; i++) {
            r[i] = (float) ((mass_total[i * times.getNumberOfTimes() + timeIndex]) / (samplesInTimeInterval[timeIndex]/*samplesPerTimeinterval*/));
//            System.out.println(getClass()+" t="+timeIndex);
        }
        return r;
    }

    public float[] getConcentrationForTimeIndex(int timeIndex) {
        float[] r = new float[distance.length];
        for (int i = 0; i < distance.length; i++) {
            r[i] = (float) ((mass_total[i * times.getNumberOfTimes() + timeIndex] * counts[i * times.getNumberOfTimes() + timeIndex]) / (volumes[i * times.getNumberOfTimes() + timeIndex] * samplesInTimeInterval[timeIndex]/*samplesPerTimeinterval*/));
        }
        return r;
    }

    public float[] getNumberOfParticlesForTimeIndex(int timeIndex) {
        float[] r = new float[distance.length];
        for (int i = 0; i < distance.length; i++) {
            r[i] = (float) ((particles[i * times.getNumberOfTimes() + timeIndex]) / (float) (samplesInTimeInterval[timeIndex]/*samplesPerTimeinterval*/));
        }
        return r;
    }

    public int[] getNumberOfMeasurementsPerTimestepForTimeIndex(int timeIndex) {
        int[] r = new int[distance.length];
        for (int i = 0; i < distance.length; i++) {
            r[i] = counts[i * times.getNumberOfTimes() + timeIndex];
        }
        return r;
    }

    public static double getMaxConcentration_global() {
        return maxConcentration_global;
    }

    public int getNumberOfCapacities() {
        return numberOfCapacities;
    }

    public boolean isTimespotmeasurement() {
        return timespotmeasurement;
    }

    public int getNumberOfContaminants() {
        return numberOfContaminants;
    }

    /**
     * Measurement timestamps can vary from the time when they should be taken.
     * This returns the simulationtime when the samples were taken.
     *
     * @param timeindex
     * @return timestamp of the sample
     */
    public long getMeasurementTimestampAtTimeIndex(int timeindex) {
        return measurementTimes[timeindex];
    }

}
