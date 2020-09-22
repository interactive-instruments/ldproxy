--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.12
-- Dumped by pg_dump version 12.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry, geography, and raster spatial types and functions';


SET default_tablespace = '';

--
-- Name: vineyards; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.vineyards (
    ogc_fid integer NOT NULL,
    wlg_nr integer,
    datum date,
    suchfeld character varying(254),
    suchfeld_1 character varying(254),
    anbaugebie character varying(254),
    bereich character varying(254),
    grosslage character varying(254),
    wlg_name character varying(254),
    gemeinde character varying(254),
    gemarkunge character varying(254),
    rebflache_ character varying(254),
    gem_info character varying(254),
    gid numeric(9,0),
    wkb_geometry public.geometry(MultiPolygon,25832)
);


ALTER TABLE public.vineyards OWNER TO postgres;

--
-- Name: weinlagen_ogc_fid_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.weinlagen_ogc_fid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.weinlagen_ogc_fid_seq OWNER TO postgres;

--
-- Name: weinlagen_ogc_fid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.weinlagen_ogc_fid_seq OWNED BY public.vineyards.ogc_fid;


--
-- Name: vineyards ogc_fid; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vineyards ALTER COLUMN ogc_fid SET DEFAULT nextval('public.weinlagen_ogc_fid_seq'::regclass);

--
-- Name: vineyards weinlagen_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vineyards
    ADD CONSTRAINT weinlagen_pkey PRIMARY KEY (ogc_fid);


--
-- Name: weinlagen_wkb_geometry_geom_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX weinlagen_wkb_geometry_geom_idx ON public.vineyards USING gist (wkb_geometry);


--
-- PostgreSQL database dump complete
--

